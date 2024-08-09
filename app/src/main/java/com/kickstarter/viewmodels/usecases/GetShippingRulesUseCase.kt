package com.kickstarter.viewmodels.usecases

import com.kickstarter.libs.Config
import com.kickstarter.libs.utils.RewardUtils
import com.kickstarter.libs.utils.extensions.getDefaultLocationFrom
import com.kickstarter.models.Location
import com.kickstarter.models.Project
import com.kickstarter.models.Reward
import com.kickstarter.models.ShippingRule
import com.kickstarter.services.ApolloClientTypeV2
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

data class ShippingRulesState(
    val shippingRules: List<ShippingRule> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val selectedShippingRule: ShippingRule = ShippingRule.builder().build(),
    val filteredRw: List<Reward> = emptyList()
)

/**
 * Will provide ShippingRulesState where:
 * `shippingRules` is the list of available shipping rules for a given project
 * `selectedShippingRule` will be the initial default shipping rule for a given configuration
 *  `error/loading` states for internal networking calls
 *
 *  Should be provided with:
 *  @param scope
 *  @param dispatcher
 *
 *  As the UseCase is lifecycle agnostic and is scoped to the class that uses it.
 */
class GetShippingRulesUseCase(
    private val apolloClient: ApolloClientTypeV2,
    private val project: Project,
    private val config: Config?,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val filteredRewards = mutableListOf<Reward>()
    private var defaultShippingRule = ShippingRule.builder().build()
    private var rewardsByShippingType: List<Reward>
    private val allAvailableRulesForProject = mutableMapOf<Long, ShippingRule>()

    init {

        // To avoid duplicates insert reward.id as key
        val rewardsToQuery = mutableMapOf<Long, Reward>()

        // Get first reward with unrestricted shipping preference, when quering `getShippingRules` will return ALL available locations, no need to query more rewards locations
        project.rewards()?.filter { RewardUtils.shipsWorldwide(reward = it) }?.firstOrNull()?.let {
            rewardsToQuery.put(it.id(), it)
        }

        // In case there is no unrestricted preference need to get restricted and local rewards, to query their specific locations
        if (rewardsToQuery.isEmpty()) {
            project.rewards()?.filter {
                RewardUtils.shipsToRestrictedLocations(reward = it)
            }?.forEach {
                rewardsToQuery[it.id()] = it
            }
        }

        this.rewardsByShippingType = rewardsToQuery.values.toList()
    }

    // - Do not expose mutable states
    private val _mutableShippingRules =
        MutableStateFlow(ShippingRulesState())

    /**
     * Exposes result of this use case
     */
    val shippingRulesState: Flow<ShippingRulesState>
        get() = _mutableShippingRules

    // - IO dispatcher for network operations to avoid blocking main thread
    operator fun invoke() {
        scope.launch(dispatcher) {
            emitCurrentState(isLoading = true)

            if (rewardsByShippingType.isNotEmpty()) {
                rewardsByShippingType.forEachIndexed { index, reward ->

                    if (RewardUtils.shipsToRestrictedLocations(reward)) {
                        reward.shippingRules()?.map {
                            allAvailableRulesForProject.put(
                                requireNotNull(
                                    it.location()?.id()
                                ),
                                it
                            )
                        }
                    }
                    if (RewardUtils.shipsWorldwide(reward)) {
                        getGlobalShippingRulesForReward(reward, allAvailableRulesForProject)
                    }

                    // - Filter if all shipping rules for all rewards have been collected
                    if (index == rewardsByShippingType.size - 1) {
                        defaultShippingRule = getDefaultShippingRule(
                            allAvailableRulesForProject,
                            project
                        )
                        filterRewardsByLocation(allAvailableRulesForProject, defaultShippingRule, project.rewards() ?: emptyList())
                    }
                }
            } else {
                // - All rewards are digital, all rewards must be available
                filteredRewards.clear()
                filteredRewards.addAll(project.rewards() ?: emptyList())
                emitCurrentState(isLoading = false)
            }
        }
    }

    fun filterBySelectedRule(shippingRule: ShippingRule) {
        scope.launch(dispatcher) {
            emitCurrentState(isLoading = true)
            delay(500) // Added delay due to the filtering happening too fast for the user to perceive
            filterRewardsByLocation(allAvailableRulesForProject, shippingRule, project.rewards() ?: emptyList())
        }
    }

    private suspend fun emitCurrentState(isLoading: Boolean, errorMessage: String? = null) {
        _mutableShippingRules.emit(
            ShippingRulesState(
                shippingRules = allAvailableRulesForProject.values.toList(),
                loading = isLoading,
                selectedShippingRule = defaultShippingRule,
                error = errorMessage,
                filteredRw = filteredRewards
            )
        )
    }

    private suspend fun getGlobalShippingRulesForReward(
        reward: Reward,
        shippingRules: MutableMap<Long, ShippingRule>
    ) {
        apolloClient.getShippingRules(reward)
            .asFlow()
            .map { rulesEnvelope ->
                rulesEnvelope.shippingRules()?.map { rule ->
                    rule?.let {
                        shippingRules.put(
                            requireNotNull(
                                it.location()?.id()
                            ),
                            it
                        )
                    }
                }
            }
            .catch { throwable ->
                emitCurrentState(isLoading = false, errorMessage = throwable?.message)
            }.collect()
    }

    /**
     * Check if the given @param rule is available in the list
     * of @param allAvailableShippingRules for this project.
     *
     * In case it is available, return only those rewards able to ship to
     * the selected rule
     */
    private suspend fun filterRewardsByLocation(
        allAvailableShippingRules: MutableMap<Long, ShippingRule>,
        rule: ShippingRule,
        rewards: List<Reward>
    ) {
        filteredRewards.clear()
        val locationId = rule.location()?.id() ?: 0
        val isIsValidRule = allAvailableShippingRules[locationId]

        // Rule is available
        rewards.map { rw ->
            if (RewardUtils.shipsWorldwide(rw)) {
                filteredRewards.add(rw)
            }

            if (RewardUtils.isNoReward(rw)) {
                filteredRewards.add(rw)
            }

            if (RewardUtils.isLocalPickup(rw)) {
                filteredRewards.add(rw)
            }

            // - If shipping is restricted, make sure the reward is able to ship to selected rule
            if (RewardUtils.shipsToRestrictedLocations(rw)) {
                if (isIsValidRule != null) {
                    rw.shippingRules()?.map {
                        if (it.location()?.id() == locationId) {
                            filteredRewards.add(rw)
                        }
                    }
                }
            }
        }

        emitCurrentState(isLoading = false)
    }

    /**
     * In case the project is backing, return the backed shippingRule
     * otherwise return the config default shippingRule
     */
    private fun getDefaultShippingRule(
        shippingRules: MutableMap<Long, ShippingRule>,
        project: Project
    ): ShippingRule =
        if (project.isBacking()) ShippingRule.builder()
            .apply {
                val locationId = project.backing()?.locationId() ?: 0L
                val locationName = project.backing()?.locationName() ?: ""

                this.location(Location.Builder().id(locationId).name(locationName).displayableName(locationName).build())
            }
            .build()
        else config?.getDefaultLocationFrom(shippingRules.values.toList()) ?: ShippingRule.builder()
            .build()
}

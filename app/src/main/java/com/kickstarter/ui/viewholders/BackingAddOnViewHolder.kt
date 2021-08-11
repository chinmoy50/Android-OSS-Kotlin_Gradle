package com.kickstarter.ui.viewholders

import android.util.Pair
import android.view.View
import androidx.annotation.NonNull
import androidx.recyclerview.widget.LinearLayoutManager
import com.kickstarter.R
import com.kickstarter.databinding.ItemAddOnPledgeBinding
import com.kickstarter.libs.rx.transformers.Transformers
import com.kickstarter.libs.utils.RewardUtils
import com.kickstarter.libs.utils.ViewUtils
import com.kickstarter.models.Reward
import com.kickstarter.models.ShippingRule
import com.kickstarter.ui.adapters.RewardItemsAdapter
import com.kickstarter.ui.data.ProjectData
import com.kickstarter.ui.views.AddOnCardComponent
import com.kickstarter.viewmodels.BackingAddOnViewHolderViewModel
import org.jsoup.internal.StringUtil

class BackingAddOnViewHolder(private val binding: ItemAddOnPledgeBinding, private val viewListener: ViewListener) : KSViewHolder(binding.root), AddOnCardComponent.AddonCardListener {

    interface ViewListener {
        fun quantityPerId(quantityPerId: Pair<Int, Long>)
    }

    private var viewModel = BackingAddOnViewHolderViewModel.ViewModel(environment())
    private val ksString = environment().ksString()

    init {

        binding.addOnCard.setAddonCardListener(this)
        val rewardItemAdapter = RewardItemsAdapter()
        binding.addOnCard.setUpItemsAdapter(rewardItemAdapter, LinearLayoutManager(context()))

//        setListenerForDecreaseButton()
//        setListenerForIncreaseButton()
//        setListenerForAddButton()

//        this.viewModel.outputs
//            .description()
//            .compose(bindToLifecycle())
//            .subscribe {
//                binding.addOnDescription.text = it
//            }

        this.viewModel.outputs.rewardItemsAreGone()
            .compose(bindToLifecycle())
            .compose(Transformers.observeForUI())
            .subscribe {
                binding.addOnCard.setAddOnItemLayoutVisibility(!it)
                binding.addOnCard.setDividerVisibility(!it)
            }

        this.viewModel.outputs.rewardItems()
            .compose(bindToLifecycle())
            .compose(Transformers.observeForUI())
            .subscribe { rewardItemAdapter.rewardsItems(it) }

        this.viewModel.outputs.titleForAddOn()
            .compose(bindToLifecycle())
            .compose(Transformers.observeForUI())
            .subscribe { binding.addOnCard.setAddOnTitleText(it) }

        this.viewModel.outputs.description()
            .compose(bindToLifecycle())
            .compose(Transformers.observeForUI())
            .subscribe { binding.addOnCard.setAddOnDescription(it) }

        this.viewModel.outputs.description()
            .filter { it.isNullOrEmpty() }
            .compose(bindToLifecycle())
            .compose(Transformers.observeForUI())
            .subscribe {
                binding.addOnCard.setAddonDescriptionVisibility(false)
            }


        this.viewModel.outputs.minimum()
            .compose(bindToLifecycle())
            .compose(Transformers.observeForUI())
            .subscribe {
                binding.addOnCard.setAddOnMinimum(it.toString())
            }

        this.viewModel.outputs.conversionIsGone()
            .compose(bindToLifecycle())
            .compose(Transformers.observeForUI())
            .subscribe { binding.addOnCard.setAddonConversionVisibility(!it) }

        this.viewModel.outputs.conversion()
            .compose(bindToLifecycle())
            .compose(Transformers.observeForUI())
            .subscribe {
                binding.addOnCard.setAddonConversionText(
                    this.ksString.format(
                        context().getString(R.string.About_reward_amount),
                        "reward_amount",
                        it.toString()
                    )
                )
            }

        this.viewModel.outputs.backerLimitPillIsGone()
            .compose(bindToLifecycle())
            .compose(Transformers.observeForUI())
            .subscribe { binding.addOnCard.setBackerLimitPillVisibility(!it) }

        this.viewModel.outputs.remainingQuantityPillIsGone()
            .compose(bindToLifecycle())
            .compose(Transformers.observeForUI())
            .subscribe { binding.addOnCard.setAddonQuantityRemainingPillVisibility(!it) }

        this.viewModel.outputs.backerLimit()
            .compose(bindToLifecycle())
            .compose(Transformers.observeForUI())
            .subscribe {
                binding.addOnCard.setBackerLimitText(
                    this.ksString.format(
                        context().getString(R.string.limit_limit_per_backer),
                        "limit_per_backer",
                        it
                    )
                )
            }

        this.viewModel.outputs.remainingQuantity()
            .compose(bindToLifecycle())
            .compose(Transformers.observeForUI())
            .subscribe {
                binding.addOnCard.setAddonQuantityRemainingText(
                    this.ksString.format(
                        context().getString(R.string.rewards_info_time_left),
                        "time",
                        it
                    )
                )
            }

        this.viewModel.outputs.deadlineCountdownIsGone()
            .compose(bindToLifecycle())
            .compose(Transformers.observeForUI())
            .subscribe {
                binding.addOnCard.setTimeLeftVisibility(!it)
            }

        this.viewModel.outputs.deadlineCountdown()
            .compose(bindToLifecycle())
            .compose(Transformers.observeForUI())
            .subscribe { binding.addOnCard.setAddonTimeLeftText(formattedExpirationString(it)) }

        this.viewModel.outputs.shippingAmountIsGone()
            .compose(bindToLifecycle())
            .compose(Transformers.observeForUI())
            .subscribe {
                binding.addOnCard.setShippingAmountVisibility(!it)
            }

        this.viewModel.outputs.shippingAmount()
            .compose(bindToLifecycle())
            .compose(Transformers.observeForUI())
            .subscribe {
                if (it.isNotEmpty()) {
                    val rewardAndShippingString =
                        context().getString(R.string.reward_amount_plus_shipping_cost_each)
                    val stringSections = rewardAndShippingString.split("+")
                    val shippingString = "+" + stringSections[1]
                    binding.addOnCard.setAddonShippingAmountText(
                        this.ksString.format(
                            shippingString,
                            "shipping_cost",
                            it
                        )
                    )
                }
            }

        this.viewModel.outputs.maxQuantity()
            .compose(bindToLifecycle())
            .compose(Transformers.observeForUI())
            .subscribe {
                binding.addOnCard.setStepperMax(it)
            }

        this.viewModel.outputs.addButtonIsGone()
            .compose(bindToLifecycle())
            .compose(Transformers.observeForUI())
            .subscribe { addButtonIsGone ->
                if (addButtonIsGone) {
                    binding.addOnCard.showStepper()
                } else {
                    binding.addOnCard.hideStepper()
                }
            }

        this.viewModel.outputs.quantityPerId()
            .compose(bindToLifecycle())
            .compose(Transformers.observeForUI())
            .subscribe { quantityPerId ->
                quantityPerId?.let { viewListener.quantityPerId(it) }
                val quantity = quantityPerId.first
                binding.addOnCard.setStepperInitialValue(quantity)
//                binding.addOnCard.setQuantityAddonText(quantity.toString())
//            }
//
//        this.viewModel.outputs.disableIncreaseButton()
//            .compose(bindToLifecycle())
//            .compose(Transformers.observeForUI())
//            .subscribe { binding.addOnCard.setIncreaseQuantityAddonEnabled(!it)
//            }
            }
    }

//    private fun hideStepper() {
//        binding.initialStateAddOn.visibility = View.VISIBLE
//        binding.stepperContainerAddOn.visibility = View.GONE
//        binding.initialStateAddOn.isEnabled = true
//        binding.increaseQuantityAddOn.isEnabled = false
//    }
//
//    private fun showStepper() {
//        binding.initialStateAddOn.visibility = View.GONE
//        binding.stepperContainerAddOn.visibility = View.VISIBLE
//        binding.initialStateAddOn.isEnabled = false
//        binding.increaseQuantityAddOn.isEnabled = true
//    }

    private fun setListenerForAddButton() {
        binding.addOnCard.getAddButtonClickListener()
            .compose(bindToLifecycle())
            .compose(Transformers.observeForUI())
            .subscribe {
                this.viewModel.inputs.addButtonPressed()
            }
//
//        binding.initialStateAddOn.setOnClickListener {
//            this.viewModel.inputs.addButtonPressed()
//        }
    }

//    private fun setListenerForIncreaseButton() {
//        binding.addOnCard.getIncreaseQuantityClickListener()
//            .compose(bindToLifecycle())
//            .compose(Transformers.observeForUI())
//            .subscribe {
//                this.viewModel.inputs.increaseButtonPressed()
//            }
//    }
//
//    private fun setListenerForDecreaseButton() {
//        binding.addOnCard.getDecreaseQuantityClickListener()
//            .compose(bindToLifecycle())
//            .compose(Transformers.observeForUI())
//            .subscribe { this.viewModel.inputs.decreaseButtonPressed() }
//    }

    private fun formattedExpirationString(@NonNull reward: Reward): String {
        val detail = RewardUtils.deadlineCountdownDetail(reward, context(), this.ksString)
        val value = RewardUtils.deadlineCountdownValue(reward)
        return "$value $detail"
    }

    override fun bindData(data: Any?) {
        if (data is (Triple<*, *, *>)) {
            if (data.second is Reward) {
                bindAddonsList(data as Triple<ProjectData, Reward, ShippingRule>)
            }
        }
    }

    private fun bindAddonsList(projectDataAndAddOn: Triple<ProjectData, Reward, ShippingRule>) {
        this.viewModel.inputs.configureWith(projectDataAndAddOn)
    }

    override fun addButtonClicks() {
        viewModel.inputs.addButtonPressed()

    }

//    override fun displayChanges(display: Int) {
//        viewModel.inputs.currentQuantity(display)
//    }

//    private fun setUpItemAdapter(): RewardItemsAdapter {
//        val rewardItemAdapter = RewardItemsAdapter()
//        binding.addOnCard.apply {
//            adapter = rewardItemAdapter
//            layoutManager = LinearLayoutManager(context())
//        }
//
//        return rewardItemAdapter
//    }
}

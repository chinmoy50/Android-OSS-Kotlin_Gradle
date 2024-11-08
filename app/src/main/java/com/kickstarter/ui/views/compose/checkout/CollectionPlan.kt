import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RadioButton
import androidx.compose.material.RadioButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kickstarter.R
import com.kickstarter.ui.compose.designsystem.KSTheme
import com.kickstarter.ui.compose.designsystem.KSTheme.colors
import com.kickstarter.ui.compose.designsystem.KSTheme.dimensions
import com.kickstarter.ui.compose.designsystem.KSTheme.typography
import androidx.compose.ui.res.stringResource

@Preview(showBackground = false, name = "Eligible - Pledge Over Time Selected")
@Composable
fun PreviewPledgeOverTimeSelected() {
    KSTheme {
        CollectionPlan(isEligible = true, initialSelectedOption = "Pledge Over Time")
    }
}

@Preview(showBackground = false, name = "Eligible - Pledge in Full Selected")
@Composable
fun PreviewPledgeInFullSelected() {
    KSTheme {
        CollectionPlan(isEligible = true, initialSelectedOption = "Pledge in full")
    }
}

@Preview(showBackground = false, name = "Not Eligible")
@Composable
fun PreviewNotEligibleComponent() {
    KSTheme {
        CollectionPlan(isEligible = false, initialSelectedOption = "Pledge in full")
    }
}

enum class CollectionPlanTestTags {
    OPTION_PLEDGE_IN_FULL,
    OPTION_PLEDGE_OVER_TIME,
    DESCRIPTION_TEXT,
    BADGE_TEXT,
    EXPANDED_TEXT,
    TERMS_TEXT,
    CHARGE_ITEM,
}

@Composable
fun CollectionPlan(isEligible: Boolean, initialSelectedOption: String = "Pledge in full") {
    var selectedOption by remember { mutableStateOf(initialSelectedOption) }

    Column(modifier = Modifier.fillMaxWidth()) {
        PledgeOption(
            optionText = stringResource(id = R.string.fpo_pledge_in_full),
            selected = selectedOption == "Pledge in full",
            onSelect = { selectedOption = "Pledge in full" },
            modifier = Modifier.testTag(CollectionPlanTestTags.OPTION_PLEDGE_IN_FULL.name)
        )
        Spacer(Modifier.height(dimensions.paddingSmall))
        PledgeOption(
            optionText = stringResource(id = R.string.fpo_pledge_over_time),
            selected = selectedOption == "Pledge Over Time",
            description = if (isEligible) stringResource(id = R.string.fpo_you_will_be_charged_for_your_pledge_over_four_payments_at_no_extra_cost) else null,
            onSelect = {
                if (isEligible) selectedOption = "Pledge Over Time"
            },
            isExpanded = selectedOption == "Pledge Over Time" && isEligible,
            isSelectable = isEligible,
            showBadge = !isEligible,
            modifier = Modifier.testTag(CollectionPlanTestTags.OPTION_PLEDGE_OVER_TIME.name)
        )
    }
}

@Composable
fun PledgeOption(
    modifier: Modifier = Modifier,
    optionText: String,
    selected: Boolean,
    description: String? = null,
    onSelect: () -> Unit,
    isExpanded: Boolean = false,
    isSelectable: Boolean = true,
    showBadge: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(dimensions.radiusSmall))
            .background(colors.kds_white)
            .clickable(enabled = isSelectable, onClick = onSelect)
            .padding(end = dimensions.paddingSmall, bottom = dimensions.paddingSmall)
            .then(
                if (!isSelectable) Modifier.padding(
                    vertical = dimensions.paddingMediumSmall,
                    horizontal = dimensions.paddingMediumSmall
                ) else Modifier
            )
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(start = dimensions.paddingSmall)
        ) {
            Column {
                RadioButton(
                    modifier = if (!isSelectable) Modifier.padding(end = dimensions.paddingMediumSmall) else Modifier,
                    selected = selected,
                    onClick = onSelect.takeIf { isSelectable },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = colors.kds_create_700,
                        unselectedColor = colors.kds_support_300
                    )
                )
            }
            Column {
                Text(
                    modifier = Modifier.padding(top = if (isSelectable) dimensions.paddingMedium else dimensions.dialogButtonSpacing),
                    text = optionText,
                    style = typography.subheadlineMedium,
                    color = if (isSelectable) colors.kds_black else colors.textDisabled
                )
                if (showBadge) {
                    Spacer(modifier = Modifier.height(dimensions.paddingXSmall))
                    PledgeBadge(modifier = Modifier.testTag(CollectionPlanTestTags.BADGE_TEXT.name))
                } else if (description != null) {
                    Text(
                        modifier = Modifier
                            .padding(bottom = dimensions.paddingSmall)
                            .testTag(CollectionPlanTestTags.DESCRIPTION_TEXT.name),
                        text = description,
                        style = typography.caption2,
                        color = colors.textDisabled
                    )
                }
                if (isExpanded) {
                    Spacer(modifier = Modifier.height(dimensions.paddingSmall))
                    Text(
                        modifier = Modifier.testTag(CollectionPlanTestTags.EXPANDED_TEXT.name),
                        text = stringResource(id = R.string.fpo_the_first_charge_will_be_24_hours_after_the_project_ends_successfully),
                        style = typography.caption2,
                        color = colors.textDisabled
                    )
                    Spacer(modifier = Modifier.height(dimensions.paddingXSmall))
                    Text(
                        modifier = Modifier.testTag(CollectionPlanTestTags.TERMS_TEXT.name),
                        text = stringResource(id = R.string.fpo_see_our_terms_of_use),
                        style = typography.caption2,
                        color = colors.textAccentGreen
                    )
                    ChargeSchedule()
                }
            }
        }
    }
}

@Composable
fun PledgeBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                color = colors.borderSubtle,
                shape = RoundedCornerShape(dimensions.radiusSmall)
            )
            .padding(
                start = dimensions.paddingSmall,
                end = dimensions.paddingSmall,
                top = dimensions.paddingXSmall,
                bottom = dimensions.paddingXSmall,
            )
    ) {
        Text(
            text = stringResource(id = R.string.fpo_available_for_pledges_over_150),
            style = typography.body2Medium,
            color = colors.textDisabled
        )
    }
}

@Composable
fun ChargeSchedule() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        ChargeItem("Charge 1", "Aug 11, 2024", "$250")
        ChargeItem("Charge 2", "Aug 15, 2024", "$250")
        ChargeItem("Charge 3", "Aug 29, 2024", "$250")
        ChargeItem("Charge 4", "Sep 12, 2024", "$250")
    }
}

@Composable
fun ChargeItem(title: String, date: String, amount: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(CollectionPlanTestTags.CHARGE_ITEM.name), // Add test tag for each charge item
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.padding(bottom = dimensions.paddingMediumLarge)) {
            Text(text = title, style = typography.body2Medium)
            Row(modifier = Modifier.padding(top = dimensions.paddingXSmall)) {
                Text(text = date, color = colors.textSecondary, style = typography.footnote)
                Spacer(modifier = Modifier.width(dimensions.paddingXLarge))
                Text(text = amount, color = colors.textSecondary, style = typography.footnote)
            }
        }
    }
}
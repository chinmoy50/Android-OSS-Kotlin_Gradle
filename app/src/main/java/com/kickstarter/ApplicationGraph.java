package com.kickstarter;

import com.kickstarter.libs.Environment;
import com.kickstarter.libs.KSCurrency;
import com.kickstarter.libs.Koala;
import com.kickstarter.libs.KoalaTrackingClient;
import com.kickstarter.libs.utils.ApplicationLifecycleUtil;
import com.kickstarter.services.gcm.MessageService;
import com.kickstarter.services.gcm.RegisterService;
import com.kickstarter.ui.activities.ActivityFeedActivity;
import com.kickstarter.ui.activities.CheckoutActivity;
import com.kickstarter.ui.activities.CommentFeedActivity;
import com.kickstarter.ui.activities.DiscoveryActivity;
import com.kickstarter.ui.activities.HelpActivity;
import com.kickstarter.ui.activities.LoginActivity;
import com.kickstarter.ui.activities.ProjectActivity;
import com.kickstarter.ui.activities.SettingsActivity;
import com.kickstarter.ui.activities.ThanksActivity;
import com.kickstarter.ui.toolbars.DiscoveryToolbar;
import com.kickstarter.ui.viewholders.ActivitySampleFriendBackingViewHolder;
import com.kickstarter.ui.viewholders.ActivitySampleFriendFollowViewHolder;
import com.kickstarter.ui.viewholders.ActivitySampleProjectViewHolder;
import com.kickstarter.ui.viewholders.CommentViewHolder;
import com.kickstarter.ui.viewholders.EmptyActivityFeedViewHolder;
import com.kickstarter.ui.viewholders.EmptyCommentFeedViewHolder;
import com.kickstarter.ui.viewholders.FriendBackingViewHolder;
import com.kickstarter.ui.viewholders.ProfileCardViewHolder;
import com.kickstarter.ui.viewholders.ProjectCardViewHolder;
import com.kickstarter.ui.viewholders.ProjectContextViewHolder;
import com.kickstarter.ui.viewholders.ProjectSearchResultViewHolder;
import com.kickstarter.ui.viewholders.ProjectStateChangedPositiveViewHolder;
import com.kickstarter.ui.viewholders.ProjectStateChangedViewHolder;
import com.kickstarter.ui.viewholders.ProjectUpdateViewHolder;
import com.kickstarter.ui.viewholders.ProjectViewHolder;
import com.kickstarter.ui.viewholders.RewardViewHolder;
import com.kickstarter.ui.viewholders.ThanksCategoryViewHolder;
import com.kickstarter.ui.viewholders.ThanksProjectViewHolder;
import com.kickstarter.ui.viewholders.discoverydrawer.ChildFilterViewHolder;
import com.kickstarter.ui.viewholders.discoverydrawer.ParentFilterViewHolder;
import com.kickstarter.ui.viewholders.discoverydrawer.TopFilterViewHolder;
import com.kickstarter.ui.views.AppRatingDialog;
import com.kickstarter.ui.views.IconTextView;
import com.kickstarter.ui.views.KSWebView;

public interface ApplicationGraph {
  Environment environment();
  void inject(ActivityFeedActivity __);
  void inject(ActivitySampleFriendBackingViewHolder __);
  void inject(ActivitySampleFriendFollowViewHolder __);
  void inject(ActivitySampleProjectViewHolder __);
  void inject(ApplicationLifecycleUtil __);
  void inject(AppRatingDialog __);
  void inject(ThanksCategoryViewHolder __);
  void inject(CheckoutActivity __);
  void inject(CommentFeedActivity __);
  void inject(CommentViewHolder __);
  void inject(Koala __);
  void inject(DiscoveryActivity __);
  void inject(DiscoveryToolbar __);
  void inject(EmptyActivityFeedViewHolder __);
  void inject(EmptyCommentFeedViewHolder __);
  void inject(FriendBackingViewHolder __);
  void inject(ChildFilterViewHolder __);
  void inject(HelpActivity __);
  void inject(IconTextView __);
  void inject(KoalaTrackingClient __);
  void inject(KSWebView __);
  void inject(KSApplication __);
  void inject(LoginActivity __);
  void inject(MessageService __);
  void inject(KSCurrency __);
  void inject(ParentFilterViewHolder __);
  void inject(ProfileCardViewHolder __);
  void inject(ProjectContextViewHolder __);
  void inject(ProjectActivity __);
  void inject(ProjectCardViewHolder __);
  void inject(ThanksProjectViewHolder __);
  void inject(ProjectSearchResultViewHolder __);
  void inject(ProjectStateChangedViewHolder __);
  void inject(ProjectStateChangedPositiveViewHolder __);
  void inject(ProjectUpdateViewHolder __);
  void inject(ProjectViewHolder __);
  void inject(RegisterService __);
  void inject(RewardViewHolder __);
  void inject(SettingsActivity __);
  void inject(ThanksActivity __);
  void inject(TopFilterViewHolder __);
}

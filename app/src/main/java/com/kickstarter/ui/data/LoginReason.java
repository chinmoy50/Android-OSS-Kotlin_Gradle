package com.kickstarter.ui.data;

import androidx.annotation.NonNull;

public enum LoginReason {
  DEFAULT,
  ACTIVITY_FEED,
  CHANGE_PASSWORD,
  COMMENT_FEED,
  CREATE_PASSWORD,
  BACK_PROJECT,
  MESSAGE_CREATOR,
  RESET_PASSWORD,
  STAR_PROJECT;

  public boolean isDefaultFlow() {
    return this == DEFAULT;
  }

  public boolean isContextualFlow() {
    return !isDefaultFlow();
  }
}

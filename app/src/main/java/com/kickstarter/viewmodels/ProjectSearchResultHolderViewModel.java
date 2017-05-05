package com.kickstarter.viewmodels;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import com.kickstarter.libs.ActivityViewModel;
import com.kickstarter.libs.Environment;
import com.kickstarter.libs.utils.ProjectUtils;
import com.kickstarter.models.Photo;
import com.kickstarter.models.Project;
import com.kickstarter.ui.viewholders.ProjectSearchResultViewHolder;

import rx.Observable;
import rx.subjects.PublishSubject;

import static com.kickstarter.libs.rx.transformers.Transformers.takeWhen;

public interface ProjectSearchResultHolderViewModel {

  final class Data {
    public final Project project;
    final boolean isFeatured;

    public Data(final @NonNull Project project, final boolean isFeatured) {
      this.project = project;
      this.isFeatured = isFeatured;
    }

    @Override
    public boolean equals(final @NonNull Object obj) {
      if (obj == null || !(obj instanceof Data)) {
        return false;
      }
      final Data data = (Data) obj;
      return data.project.equals(this.project) && data.isFeatured == this.isFeatured;
    }

    @Override
    public int hashCode() {
      return 31 * this.project.hashCode() + (this.isFeatured ? 1 : 0);
    }
  }

  interface Inputs {
    /** Call to configure the view model with a project and isFeatured data. */
    void configureWith(Data data);

    /** Call to say user clicked a project */
    void projectClicked();
  }

  interface Outputs {
    /** Emits the project photo url to be displayed. */
    Observable<String> projectPhotoUrl();

    /** Emits title of project. */
    Observable<String> projectName();

    /** Emits a completed / days to go pair. */
    Observable<Pair<Integer, Integer>> projectStats();

    /** Emits the project clicked by the user. */
    Observable<Project> notifyDelegateOfResultClick();
  }

  final class ViewModel extends ActivityViewModel<ProjectSearchResultViewHolder> implements Inputs, Outputs {

    public ViewModel(final @NonNull Environment environment) {
      super(environment);

      this.projectPhotoUrl = this.configData
        .map(ViewModel::photoUrl);

      this.projectName = this.configData
        .map(data -> data.project.name());

      this.projectStats = this.configData
        .map(data ->
          Pair.create((int) data.project.percentageFunded(), ProjectUtils.deadlineCountdownValue(data.project))
        );

      this.notifyDelegateOfResultClick = this.configData
        .map(data -> data.project)
        .compose(takeWhen(this.projectClicked));
    }

    private final PublishSubject<Data> configData = PublishSubject.create();
    private final PublishSubject<Void> projectClicked = PublishSubject.create();

    private final Observable<Project> notifyDelegateOfResultClick;
    private final Observable<String> projectPhotoUrl;
    private final Observable<String> projectName;
    private final Observable<Pair<Integer, Integer>> projectStats;

    public final Inputs inputs = this;
    public final Outputs outputs = this;

    @Override public void configureWith(final @NonNull Data data) {
      this.configData.onNext(data);
    }
    @Override public void projectClicked() {
      this.projectClicked.onNext(null);
    }

    @Override public Observable<Project> notifyDelegateOfResultClick() {
      return this.notifyDelegateOfResultClick;
    }
    @Override public Observable<String> projectPhotoUrl() {
      return this.projectPhotoUrl;
    }
    @Override public Observable<String> projectName() {
      return this.projectName;
    }
    @Override public Observable<Pair<Integer, Integer>> projectStats() {
      return this.projectStats;
    }

    private static @Nullable String photoUrl(final @NonNull Data data) {
      final Photo photo = data.project.photo();
      if (photo == null) {
        return null;
      }

      return data.isFeatured ? photo.full() : photo.med();
    }
  }
}

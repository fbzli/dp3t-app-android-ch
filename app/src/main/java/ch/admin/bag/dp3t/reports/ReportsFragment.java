/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package ch.admin.bag.dp3t.reports;

import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.transition.AutoTransition;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.dpppt.android.sdk.models.ExposureDay;

import ch.admin.bag.dp3t.R;
import ch.admin.bag.dp3t.home.model.TracingStatusInterface;
import ch.admin.bag.dp3t.storage.SecureStorage;
import ch.admin.bag.dp3t.util.DateUtils;
import ch.admin.bag.dp3t.util.NotificationUtil;
import ch.admin.bag.dp3t.util.PhoneUtil;
import ch.admin.bag.dp3t.util.UrlUtil;
import ch.admin.bag.dp3t.viewmodel.TracingViewModel;

public class ReportsFragment extends Fragment {

	public static ReportsFragment newInstance() {
		return new ReportsFragment();
	}

	private final int DAYS_TO_STAY_IN_QUARANTINE = 10;
	private TracingViewModel tracingViewModel;
	private SecureStorage secureStorage;

	private View headerFragmentContainer;
	private LockableScrollView scrollView;
	private View scrollViewFirstchild;

	private View healthyView;
	private View saveOthersView;
	private View hotlineView;
	private View infectedView;

	private TextView callHotlineLastText1;
	private TextView callHotlineLastText2;

	private TextView xDaysLeftTextview;


	private boolean hotlineJustCalled = false;

	private int originalFirstChildPadding = 0;

	public ReportsFragment() { super(R.layout.fragment_reports); }

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		tracingViewModel = new ViewModelProvider(requireActivity()).get(TracingViewModel.class);
		secureStorage = SecureStorage.getInstance(getContext());
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		Toolbar toolbar = view.findViewById(R.id.reports_toolbar);
		toolbar.setNavigationOnClickListener(v -> getParentFragmentManager().popBackStack());

		headerFragmentContainer = view.findViewById(R.id.header_fragment_container);
		scrollView = view.findViewById(R.id.reports_scrollview);
		scrollViewFirstchild = view.findViewById(R.id.reports_scrollview_firstChild);

		healthyView = view.findViewById(R.id.reports_healthy);
		saveOthersView = view.findViewById(R.id.reports_save_others);
		hotlineView = view.findViewById(R.id.reports_hotline);
		infectedView = view.findViewById(R.id.reports_infected);

		callHotlineLastText1 = hotlineView.findViewById(R.id.card_encounters_last_call);
		callHotlineLastText2 = saveOthersView.findViewById(R.id.card_encounters_last_call);
		xDaysLeftTextview = saveOthersView.findViewById(R.id.x_days_left_textview);

		Button callHotlineButton1 = hotlineView.findViewById(R.id.card_encounters_button);
		Button callHotlineButton2 = saveOthersView.findViewById(R.id.card_encounters_button);

		callHotlineButton1.setOnClickListener(view1 -> callHotline());
		callHotlineButton2.setOnClickListener(view1 -> callHotline());

		Button faqButton1 = healthyView.findViewById(R.id.card_encounters_faq_button);
		Button faqButton2 = saveOthersView.findViewById(R.id.card_encounters_faq_button);
		Button faqButton3 = hotlineView.findViewById(R.id.card_encounters_faq_button);
		Button faqButton4 = infectedView.findViewById(R.id.card_encounters_faq_button);

		faqButton1.setOnClickListener(v -> showFaq());
		faqButton2.setOnClickListener(v -> showFaq());
		faqButton3.setOnClickListener(v -> showFaq());
		faqButton4.setOnClickListener(v -> showFaq());

		View infoLinkHealthy = healthyView.findViewById(R.id.card_encounters_link);

		infoLinkHealthy.setOnClickListener(v -> openLink(R.string.no_meldungen_box_url));

		tracingViewModel.getAppStatusLiveData().observe(getViewLifecycleOwner(), tracingStatusInterface -> {
			healthyView.setVisibility(View.GONE);
			saveOthersView.setVisibility(View.GONE);
			hotlineView.setVisibility(View.GONE);
			infectedView.setVisibility(View.GONE);

			ReportsHeaderFragment.Type headerType;
			ArrayList<Long> dates = new ArrayList<>();

			if (tracingStatusInterface.isReportedAsInfected()) {
				headerType = ReportsHeaderFragment.Type.POSITIVE_TESTED;
				infectedView.setVisibility(View.VISIBLE);
				dates.add(secureStorage.getInfectedDate());
				infectedView.findViewById(R.id.delete_reports).setOnClickListener(v -> {
					AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.NextStep_AlertDialogStyle);
					builder.setMessage(R.string.delete_infection_dialog)
							.setPositiveButton(R.string.android_button_ok, (dialog, id) -> {
								tracingStatusInterface.resetInfectionStatus(getContext());
								getParentFragmentManager().popBackStack();
							})
							.setNegativeButton(R.string.cancel, (dialog, id) -> {
								//do nothing
							});
					builder.create();
					builder.show();
				});
				if (!tracingStatusInterface.canInfectedStatusBeReset(getContext())) {
					infectedView.findViewById(R.id.delete_reports).setVisibility(View.GONE);
				}
			} else if (tracingStatusInterface.wasContactReportedAsExposed()) {
				headerType = ReportsHeaderFragment.Type.POSSIBLE_INFECTION;
				List<ExposureDay> exposureDays = tracingStatusInterface.getExposureDays();
				boolean isHotlineCallPending = secureStorage.isHotlineCallPending();
				if (isHotlineCallPending) {
					hotlineView.setVisibility(View.VISIBLE);
				} else {
					saveOthersView.setVisibility(View.VISIBLE);
				}
				for (int i = 0; i < exposureDays.size(); i++) {
					ExposureDay exposureDay = exposureDays.get(i);
					long exposureTimestamp = exposureDay.getExposedDate().getStartOfDay(TimeZone.getDefault());
					dates.add(exposureTimestamp);
				}

				int daysLeft = DAYS_TO_STAY_IN_QUARANTINE - (int) tracingStatusInterface.getDaysSinceExposure();
				if (daysLeft > DAYS_TO_STAY_IN_QUARANTINE || daysLeft <= 0) {
					xDaysLeftTextview.setVisibility(View.GONE);
				} else if (daysLeft == 1) {
					xDaysLeftTextview.setText(R.string.date_in_one_day);
				} else {
					xDaysLeftTextview.setText(getString(R.string.date_in_days).replace("{COUNT}", String.valueOf(daysLeft)));
				}

				hotlineView.findViewById(R.id.delete_reports).setOnClickListener(v -> deleteNotifications(tracingStatusInterface));
				saveOthersView.findViewById(R.id.delete_reports)
						.setOnClickListener(v -> deleteNotifications(tracingStatusInterface));
			} else {
				healthyView.setVisibility(View.VISIBLE);
				headerType = ReportsHeaderFragment.Type.NO_REPORTS;
			}

			setupHeaderFragment(headerType, dates);
		});

		NotificationManager notificationManager =
				(NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(NotificationUtil.NOTIFICATION_ID_CONTACT);
	}


	private void deleteNotifications(TracingStatusInterface tracingStatusInterface) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.NextStep_AlertDialogStyle);
		builder.setMessage(R.string.delete_reports_dialog)
				.setPositiveButton(R.string.android_button_ok, (dialog, id) -> {
					tracingStatusInterface.resetExposureDays(getContext());
					getParentFragmentManager().popBackStack();
				})
				.setNegativeButton(R.string.cancel, (dialog, id) -> {
					//do nothing
				});
		builder.create();
		builder.show();
	}

	private void openLink(@StringRes int stringRes) {
		UrlUtil.openUrl(getContext(), getString(stringRes));
	}

	private void showFaq() {
		UrlUtil.openUrl(getContext(), getString(R.string.faq_button_url));
	}

	private void callHotline() {
		hotlineJustCalled = true;
		secureStorage.justCalledHotline();
		PhoneUtil.callHelpline(getContext());
	}

	@Override
	public void onResume() {
		super.onResume();

		if (hotlineJustCalled) {
			hotlineJustCalled = false;
			hotlineView.setVisibility(View.GONE);
			saveOthersView.setVisibility(View.VISIBLE);
		}

		long lastHotlineCallTimestamp = secureStorage.lastHotlineCallTimestamp();
		if (lastHotlineCallTimestamp != 0) {
			((TextView) hotlineView.findViewById(R.id.card_encounters_title)).setText(R.string.meldungen_detail_call_again);

			String date = DateUtils.getFormattedDateTime(lastHotlineCallTimestamp);
			date = getString(R.string.meldungen_detail_call_last_call).replace("{DATE}", date);
			callHotlineLastText1.setText(date);
			callHotlineLastText2.setText(date);
		} else {
			callHotlineLastText1.setText("");
			callHotlineLastText2.setText("");
		}
	}


	public void doHeaderAnimation(View info, View image, Button button, View showAllButton, int numExposureDays) {
		secureStorage.setReportsHeaderAnimationPending(false);

		ViewGroup rootView = (ViewGroup) getView();

		scrollViewFirstchild.setPadding(scrollViewFirstchild.getPaddingLeft(),
				rootView.getHeight(),
				scrollViewFirstchild.getPaddingRight(), scrollViewFirstchild.getPaddingBottom());
		scrollViewFirstchild.setVisibility(View.VISIBLE);

		rootView.post(() -> {

			AutoTransition autoTransition = new AutoTransition();
			autoTransition.setDuration(300);
			autoTransition.addListener(new Transition.TransitionListener() {
				@Override
				public void onTransitionStart(@NonNull Transition transition) {

				}

				@Override
				public void onTransitionEnd(@NonNull Transition transition) {
					headerFragmentContainer.post(() -> {
						setupScrollBehavior();
					});
				}

				@Override
				public void onTransitionCancel(@NonNull Transition transition) {

				}

				@Override
				public void onTransitionPause(@NonNull Transition transition) {

				}

				@Override
				public void onTransitionResume(@NonNull Transition transition) {

				}
			});

			TransitionManager.beginDelayedTransition(rootView, autoTransition);

			updateHeaderSize(false, numExposureDays);

			info.setVisibility(View.VISIBLE);
			image.setVisibility(View.GONE);
			button.setVisibility(View.GONE);
			if (numExposureDays <= 1) {
				showAllButton.setVisibility(View.GONE);
			} else {
				showAllButton.setVisibility(View.VISIBLE);
			}
		});
	}

	public void animateHeaderHeight(boolean showAll, int numExposureDays, View exposureDaysContainer, View dateTextView) {

		int exposureDayItemHeight = getResources().getDimensionPixelSize(R.dimen.header_reports_exposure_day_height);
		int endHeaderHeight;
		int endDateTextHeight;
		int endExposureDaysContainerHeight;
		int endScrollViewPadding;
		if (showAll) {
			endHeaderHeight = getResources().getDimensionPixelSize(R.dimen.header_height_reports_multiple_days) +
					exposureDayItemHeight * (numExposureDays - 1);
			endDateTextHeight = 0;
			endExposureDaysContainerHeight = exposureDayItemHeight * numExposureDays;
			endScrollViewPadding = getResources().getDimensionPixelSize(R.dimen.top_item_padding_reports_multiple_days) +
					exposureDayItemHeight * (numExposureDays - 1);
		} else {
			endHeaderHeight = getResources().getDimensionPixelSize(R.dimen.header_height_reports_multiple_days);
			endDateTextHeight = exposureDayItemHeight;
			endExposureDaysContainerHeight = 0;
			endScrollViewPadding = getResources().getDimensionPixelSize(R.dimen.top_item_padding_reports_multiple_days);
		}

		int startHeaderHeight = headerFragmentContainer.getLayoutParams().height;
		int startScrollViewPadding = scrollViewFirstchild.getPaddingTop();
		int startDateTextHeight = dateTextView.getLayoutParams().height;
		int startExposureDaysContainerHeight = exposureDaysContainer.getLayoutParams().height;

		ValueAnimator anim = ValueAnimator.ofFloat(0, 1);
		anim.addUpdateListener((v) -> {
					float value = (float) v.getAnimatedValue();
					setHeight(headerFragmentContainer, value * (endHeaderHeight - startHeaderHeight) + startHeaderHeight);
					setHeight(dateTextView, value * (endDateTextHeight - startDateTextHeight) + startDateTextHeight);
					setHeight(exposureDaysContainer,
							value * (endExposureDaysContainerHeight - startExposureDaysContainerHeight) + startExposureDaysContainerHeight);
					scrollViewFirstchild.setPadding(scrollViewFirstchild.getPaddingLeft(),
							(int) (value * (endScrollViewPadding - startScrollViewPadding) + startScrollViewPadding),
							scrollViewFirstchild.getPaddingRight(), scrollViewFirstchild.getPaddingBottom());
					if (value == 0) {
						exposureDaysContainer.setVisibility(View.VISIBLE);
						dateTextView.setVisibility(View.VISIBLE);
					} else if (value == 1) {
						if (showAll) {
							dateTextView.setVisibility(View.GONE);
						} else {
							exposureDaysContainer.setVisibility(View.GONE);
						}
						headerFragmentContainer.post(this::setupScrollBehavior);
					}
				}
		);
		anim.setDuration(100);
		anim.start();
	}

	private void setHeight(View view, float height) {
		ViewGroup.LayoutParams params = view.getLayoutParams();
		params.height = (int) height;
		view.setLayoutParams(params);
	}

	private void updateHeaderSize(boolean isReportsHeaderAnimationPending, int numExposureDays) {
		ViewGroup.LayoutParams headerLp = headerFragmentContainer.getLayoutParams();
		if (isReportsHeaderAnimationPending) {
			headerLp.height = ViewGroup.LayoutParams.MATCH_PARENT;
		} else if (numExposureDays <= 1) {
			headerLp.height = getResources().getDimensionPixelSize(R.dimen.header_height_reports);
			scrollViewFirstchild.setPadding(scrollViewFirstchild.getPaddingLeft(),
					getResources().getDimensionPixelSize(R.dimen.top_item_padding_reports),
					scrollViewFirstchild.getPaddingRight(), scrollViewFirstchild.getPaddingBottom());
		} else {
			headerLp.height = getResources().getDimensionPixelSize(R.dimen.header_height_reports_multiple_days);
			scrollViewFirstchild.setPadding(scrollViewFirstchild.getPaddingLeft(),
					getResources().getDimensionPixelSize(R.dimen.top_item_padding_reports_multiple_days),
					scrollViewFirstchild.getPaddingRight(), scrollViewFirstchild.getPaddingBottom());
		}
		headerFragmentContainer.setLayoutParams(headerLp);
		headerFragmentContainer.post(() -> setupScrollBehavior());
	}

	private void setupScrollBehavior() {
		if (!isVisible()) return;

		Rect rect = new Rect();
		headerFragmentContainer.getDrawingRect(rect);
		scrollView.setScrollPreventRect(rect);

		int scrollRangePx = scrollViewFirstchild.getPaddingTop();
		int translationRangePx = -getResources().getDimensionPixelSize(R.dimen.spacing_huge);
		scrollView.setOnScrollChangeListener((v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
			float progress = computeScrollAnimProgress(scrollY, scrollRangePx);
			headerFragmentContainer.setAlpha(1 - progress);
			headerFragmentContainer.setTranslationY(progress * translationRangePx);
		});
		scrollView.post(() -> {
			float progress = computeScrollAnimProgress(scrollView.getScrollY(), scrollRangePx);
			headerFragmentContainer.setAlpha(1 - progress);
			headerFragmentContainer.setTranslationY(progress * translationRangePx);
		});
	}

	private float computeScrollAnimProgress(int scrollY, int scrollRange) {
		return Math.min(scrollY, scrollRange) / (float) scrollRange;
	}

	private void setupHeaderFragment(ReportsHeaderFragment.Type headerType, List<Long> timestamps) {

		boolean isReportsHeaderAnimationPending = secureStorage.isReportsHeaderAnimationPending();

		updateHeaderSize(isReportsHeaderAnimationPending, timestamps.size());

		if (isReportsHeaderAnimationPending) {
			originalFirstChildPadding = scrollViewFirstchild.getPaddingTop();
			scrollViewFirstchild.setVisibility(View.GONE);
		}

		headerFragmentContainer.post(this::setupScrollBehavior);

		//TODO: Check what is happening with position == items.size() - 1
		//boolean showAnimationControls = isReportsHeaderAnimationPending && position == items.size() - 1;
		boolean showAnimationControls = isReportsHeaderAnimationPending;

		Fragment header;
		switch (headerType) {
			case NO_REPORTS:
				header = ReportsHeaderFragment.newInstance(ReportsHeaderFragment.Type.NO_REPORTS, null, false);
				break;
			case POSSIBLE_INFECTION:
				header = ReportsHeaderFragment
						.newInstance(ReportsHeaderFragment.Type.POSSIBLE_INFECTION, timestamps, showAnimationControls);
				break;
			case POSITIVE_TESTED:
				header = ReportsHeaderFragment.newInstance(ReportsHeaderFragment.Type.POSITIVE_TESTED, timestamps, false);
				break;
			default:
				throw new IllegalStateException("Unexpected value: " + headerType);
		}

		getChildFragmentManager().beginTransaction()
				.replace(R.id.header_fragment_container, header)
				.commit();
	}

}

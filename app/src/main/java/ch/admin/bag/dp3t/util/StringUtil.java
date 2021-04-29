/*
 * Copyright (c) 2020 Ubique Innovation AG <https://www.ubique.ch>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * SPDX-License-Identifier: MPL-2.0
 */
package ch.admin.bag.dp3t.util;

import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import androidx.annotation.NonNull;

import java.math.BigInteger;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class StringUtil {

	private static final long ONE_HOUR = TimeUnit.HOURS.toMillis(1);

	/**
	 * Creates a spannable where the {@code boldString} is set to bold within the {@code fullString}.
	 * Be aware that this only applies to the first occurence.
	 * @param fullString The entire string
	 * @param boldString The partial string to be made bold
	 * @return A partially bold spannable
	 */
	public static Spannable makePartiallyBold(@NonNull String fullString, @NonNull String boldString) {
		int start = fullString.indexOf(boldString);
		if (start >= 0) {
			return makePartiallyBold(fullString, start, start + boldString.length());
		}
		return new SpannableString(fullString);
	}

	public static SpannableString makePartiallyBold(@NonNull String string, int start, int end) {
		SpannableString result = new SpannableString(string);
		result.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
		return result;
	}

	public static String toHex(byte[] array) {
		BigInteger bi = new BigInteger(1, array);
		String hex = bi.toString(16);
		int paddingLength = (array.length * 2) - hex.length();
		if (paddingLength > 0)
			return String.format("%0" + paddingLength + "d", 0) + hex;
		else
			return hex;
	}

	public static String getHourMinuteTimeString(long timeStamp, String delimiter) {
		Calendar time = Calendar.getInstance();
		time.setTimeInMillis(timeStamp);
		return prependZero(time.get(Calendar.HOUR_OF_DAY)) + delimiter + prependZero(time.get(Calendar.MINUTE));
	}

	private static String prependZero(int timeUnit) {
		if (timeUnit < 10) {
			return "0" + timeUnit;
		} else {
			return String.valueOf(timeUnit);
		}
	}

	/**
	 * Formats a duration in milliseconds to a String of hours, minutes and seconds, or to only hours and minutes if the
	 * duration is more than 10 hours
	 * @param duration in milliseconds
	 * @return a formatted duration String
	 */
	public static String getShortDurationString(long duration) {
		if (duration >= TimeUnit.HOURS.toMillis(10)) {
			return String.format(Locale.GERMAN, "%d:%02d",
					TimeUnit.MILLISECONDS.toHours(duration),
					TimeUnit.MILLISECONDS.toMinutes(duration - TimeUnit.HOURS.toMillis(TimeUnit.MILLISECONDS.toHours(duration)))
			);
		} else {
			return getDurationString(duration);
		}
	}

	public static String getDurationString(long duration) {
		if (duration >= ONE_HOUR) {
			return String.format(Locale.GERMAN, "%d:%02d:%02d",
					TimeUnit.MILLISECONDS.toHours(duration),
					TimeUnit.MILLISECONDS.toMinutes(duration - TimeUnit.HOURS.toMillis(TimeUnit.MILLISECONDS.toHours(duration))),
					TimeUnit.MILLISECONDS.toSeconds(duration - TimeUnit.MINUTES.toMillis(TimeUnit.MILLISECONDS.toMinutes(duration)))
			);
		} else {
			return String.format(Locale.GERMAN, "%02d:%02d",
					TimeUnit.MILLISECONDS.toMinutes(duration),
					TimeUnit.MILLISECONDS.toSeconds(duration - TimeUnit.MINUTES.toMillis(TimeUnit.MILLISECONDS.toMinutes(duration)))
			);
		}
	}

}

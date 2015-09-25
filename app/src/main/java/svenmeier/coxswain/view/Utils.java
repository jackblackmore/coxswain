/*
 * Copyright 2015 Sven Meier
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package svenmeier.coxswain.view;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.DisplayMetrics;
import android.widget.BaseAdapter;
import android.widget.ListView;

import java.util.Map;

/**
 * Created by sven on 19.08.15.
 */
public class Utils {

    public static int dpToPx(Context context, int dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();

        return (int)((dp * displayMetrics.density) + 0.5);
    }

    @SuppressWarnings("unchecked")
    public static <T> T getParent(Fragment fragment, Class<T> t) {
        Fragment parentFragment = fragment.getParentFragment();
        if (parentFragment != null && t.isInstance(parentFragment)) {
            return (T) parentFragment;
        }

        FragmentActivity activity = fragment.getActivity();
        if (activity != null && t.isInstance(activity)) {
            return (T) activity;
        }

        throw new IllegalStateException("fragment does not have requested parent " + t.getSimpleName());
    }
}

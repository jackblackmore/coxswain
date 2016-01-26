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

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Date;

import propoid.db.Order;
import propoid.ui.Index;
import propoid.ui.list.MatchAdapter;
import svenmeier.coxswain.Gym;
import svenmeier.coxswain.R;
import svenmeier.coxswain.gym.Workout;


public class WorkoutsFragment extends Fragment {

    private Gym gym;

    private ListView workoutsView;

    private WorkoutsAdapter adapter;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        gym = Gym.instance(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.layout_workouts, container, false);

        workoutsView = (ListView) root.findViewById(R.id.workouts);
        adapter = new WorkoutsAdapter();
        adapter.install(workoutsView);

        return root;
    }

    @Override
    public void onStart() {
        super.onStart();

        adapter.initLoader(0, this);
    }

    @Override
    public void onStop() {
        super.onStop();

        adapter.destroy(0, this);
    }

    private class WorkoutsAdapter extends MatchAdapter<Workout> {

        private DateFormat format = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.SHORT);

        public WorkoutsAdapter() {
            super(R.layout.layout_workouts_item, Gym.instance(getActivity()).getWorkouts());

            setOrder(Order.descending(getMatch().getPrototype().start));
        }

        @Override
        protected void bind(int position, View view, Workout workout) {
            Index index = Index.get(view);

            TextView startView = index.get(R.id.workout_start);
            startView.setText(format.format(new Date(workout.start.get())));

            TextView nameView = index.get(R.id.workout_name);
            nameView.setText(workout.name.get());

            TextView countsView = index.get(R.id.workout_counts);
            String counts = TextUtils.join(", ", new String[]{
                    String.format(getString(R.string.duration_minutes), workout.duration.get() / 60),
                    String.format(getString(R.string.distance_meters), workout.distance.get()),
                    String.format(getString(R.string.strokes_count), workout.strokes.get())
            });
            countsView.setText(counts);

            TextView caloriesView = index.get(R.id.workout_calories);
            caloriesView.setText(String.format(getString(R.string.energy_calories), workout.energy.get()));
        }
    }
}

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
package svenmeier.coxswain;

import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import propoid.db.Match;
import propoid.db.Reference;
import propoid.db.Repository;
import propoid.db.aspect.Row;
import propoid.db.cascading.DefaultCascading;
import svenmeier.coxswain.gym.Difficulty;
import svenmeier.coxswain.gym.Program;
import svenmeier.coxswain.gym.Segment;
import svenmeier.coxswain.gym.Snapshot;
import svenmeier.coxswain.gym.Workout;

public class Gym {

    private static Gym instance;

    private Context context;

    private Repository repository;

    public Program program;

    public Workout workout;

    public Snapshot snapshot;

    public Current current;

    private Gym(Context context) {

        this.context = context;

        repository = new Repository(context, "gym");
        ((DefaultCascading)repository.cascading).setCascaded(new Program().segments);

        Match<Program> query = repository.query(new Program());
        if (query.count() == 0) {
            defaultPrograms();
        }

        PreferenceManager.setDefaultValues(context, R.xml.preferences, true);
    }

    private void defaultPrograms() {
        repository.insert(Program.meters(String.format(context.getString(R.string.distance_meters), 500), 500));
        repository.insert(Program.meters(String.format(context.getString(R.string.distance_meters), 1000), 1000));
        repository.insert(Program.meters(String.format(context.getString(R.string.distance_meters), 2000), 2000));
        repository.insert(Program.meters(String.format(context.getString(R.string.distance_meters), 5000), 5000));

        repository.insert(Program.minutes(String.format(context.getString(R.string.duration_minutes), 5), 5));
        repository.insert(Program.minutes(String.format(context.getString(R.string.duration_minutes), 10), 10));
        repository.insert(Program.minutes(String.format(context.getString(R.string.duration_minutes), 30), 30));

        Program program = new Program(context.getString(R.string.segments));
        program.getSegment(0).setDistance(1000);
        program.addSegment(new Segment(Difficulty.HARD).setDuration(60).setStrokeRate(30));
        program.addSegment(new Segment(Difficulty.EASY).setDistance(1000));
        program.addSegment(new Segment(Difficulty.HARD).setDuration(60).setStrokeRate(30));
        program.addSegment(new Segment(Difficulty.EASY).setDistance(1000));
        program.addSegment(new Segment(Difficulty.HARD).setDuration(60).setStrokeRate(30));
        program.addSegment(new Segment(Difficulty.EASY).setDistance(1000));
        program.addSegment(new Segment(Difficulty.HARD).setDuration(60).setStrokeRate(30));
        program.addSegment(new Segment(Difficulty.EASY).setDistance(1000));
        repository.insert(program);
    }

    public Match<Program> getPrograms() {
        return repository.query(new Program());
    }

    public Program getProgram(Reference<Program> reference) {
        return repository.lookup(reference);
    }

    public void mergeProgram(Program program) {
        repository.merge(program);
    }

    public void deleteProgram(Program program) {
        repository.delete(program);
    }

    public Match<Workout> getWorkouts() {
        return repository.query(new Workout());
    }

    public void mergeWorkout(Workout workout) {
        repository.merge(workout);
    }

    public void select(Program program) {
        this.program = program;

        this.snapshot = null;

        if (program == null) {
            workout = null;

            current = null;
        } else {
            workout = new Workout(program);

            current = new Current(program.getSegment(0), 0, new Snapshot());
        }
    }

    public boolean isSelected(Program program) {
        return this.program != null && Row.getID(this.program) == Row.getID(program) && current != null;
    }

    public Event addSnapshot(Snapshot snapshot) {
        Event event = Event.REJECTED;

        if (current != null) {
            event = Event.SNAPPED;

            if (this.snapshot == null) {
                // first snapshot -> start of program
                event = Event.PROGRAM_START;
            }
            this.snapshot = snapshot;

            workout.onSnapshot(snapshot);

            if (current.completion() == 1.0f) {
                Segment next = program.getNextSegment(current.segment);
                if (next == null) {
                    current = null;

                    event = Event.PROGRAM_FINISHED;
                } else {
                    current = new Current(next, workout.duration.get(), snapshot);

                    event = Event.SEGMENT_CHANGED;
                }

                mergeWorkout(workout);
            }
        }

        return event;
    }

    public Snapshot getLastSnapshot() {
        return snapshot;
    }

    public class Current {

        public final Segment segment;

        final int duration;

        final Snapshot snapshot;

        public Current(Segment segment, int duration, Snapshot snapshot) {
            this.segment = segment;
            this.duration = duration;
            this.snapshot = snapshot;
        }

        public float completion() {
            float achieved = achieved();
            float target = segment.getTarget();

            return Math.min(achieved / target, 1.0f);
        }

        public int achieved() {
            int lastDuration = workout.duration.get();
            Snapshot lastSnapshot = getLastSnapshot();
            if (lastSnapshot == null) {
                return 0;
            }

            return achieved(lastSnapshot, lastDuration) - achieved(snapshot, duration);
        }

        private int achieved(Snapshot snapshot, int duration) {
            if (segment.distance.get() > 0) {
                return snapshot.distance;
            } else if (segment.strokes.get() > 0) {
                return snapshot.strokes;
            } else if (segment.duration.get() > 0){
                return duration;
            }
            return 0;
        }

        public boolean inLimit() {
            Snapshot lastSnapshot = getLastSnapshot();

            if (lastSnapshot.speed < current.segment.speed.get()) {
                return false;
            } else if (lastSnapshot.pulse < current.segment.pulse.get()) {
                return false;
            } else if (lastSnapshot.strokeRate < current.segment.strokeRate.get()) {
                return false;
            }

            return true;
        }

        public String describeTarget() {
            StringBuilder description = new StringBuilder();

            if (segment.distance.get() > 0) {
                description.append(String.format(context.getString(R.string.distance_meters), segment.distance.get()));
            } else if (segment.strokes.get() > 0) {
                description.append(String.format(context.getString(R.string.strokes_count), segment.strokes.get()));
            } else if (segment.duration.get() > 0) {
                description.append(String.format(context.getString(R.string.duration_minutes), Math.round(segment.duration.get() / 60f)));
            }
            return description.toString();
        }

        public String describeLimit() {
            StringBuilder description = new StringBuilder();

            if (segment.strokeRate.get() > 0) {
                description.append(String.format(context.getString(R.string.strokeRate_strokesPerMinute), segment.strokeRate.get()));
            } else if (segment.speed.get() > 0) {
                description.append(String.format(context.getString(R.string.speed_metersPerSecond), Math.round(segment.speed.get() / 100f)));
            } else if (segment.pulse.get() > 0){
                description.append(String.format(context.getString(R.string.pulse_beatsPerMinute), segment.pulse.get()));
            }

            return description.toString();
        }
    }

    public static Gym instance(Context context) {
        if (instance == null) {
            instance = new Gym(context.getApplicationContext());
        }

        return instance;
    }

}
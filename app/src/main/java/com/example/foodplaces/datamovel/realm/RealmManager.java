package com.example.foodplaces.datamovel.realm;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.foodplaces.BuildConfig;
import com.example.foodplaces.viewmodel.IPlace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.function.Consumer;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmResults;

public class RealmManager {
    private static final String TAG = RealmManager.class.getSimpleName();

    private static final String REALM_NAME = "FoodPlacesProjectData";
    private final OnResultListener onResultListener;
    private final Map<String, PlaceRealm> writeData = new HashMap<>();
    private final ExecutorService executorService;
    private Realm uiThreadRealm;

    public RealmManager(OnResultListener onResultListener) {
        this.onResultListener = onResultListener;
        executorService = Executors.newFixedThreadPool(2);
    }

    public void init(Activity activity) {
        Realm.init(activity);
        RealmConfiguration config = new RealmConfiguration.Builder().name(REALM_NAME).build();
        uiThreadRealm = Realm.getInstance(config);
    }

    public void writeData(@NonNull List<IPlace> data) {
        Log.i(TAG, "writeData");
        List<PlaceRealm> newData = convertToPlaceRealm(data);

        writeData.clear();
        newData.forEach(placeRealm -> writeData.put(placeRealm.getId(), placeRealm));
        FutureTask<String> Task = new FutureTask<>(new BackgroundWrite(), "test");
        executorService.execute(Task);
    }

    @NonNull
    private List<PlaceRealm> convertToPlaceRealm(@NonNull List<IPlace> data) {
        List<PlaceRealm> newData = new ArrayList<>();
        data.forEach(iPlace -> newData.add(convertToPlaceRealm(iPlace)));
        return newData;
    }

    private PlaceRealm convertToPlaceRealm(IPlace place) {
        return place instanceof PlaceRealm ? (PlaceRealm) place : new PlaceRealm(place);
    }

    public void readData() {
        Log.i(TAG, "readData");
        FutureTask<String> Task = new FutureTask<>(new BackgroundRead(), "test");
        executorService.execute(Task);
    }

    public void close() {
        uiThreadRealm.close();
        executorService.shutdown();
    }

    public interface OnResultListener {
        void onWriteSuccessfully();

        void onReadResult(List<PlaceRealm> data);
    }

    public class BackgroundWrite implements Runnable {
        @Override
        public void run() {
            Log.i(TAG, "BackgroundWrite-run: start");
            try {
                RealmConfiguration config = new RealmConfiguration.Builder().name(REALM_NAME).build();
                Realm backgroundThreadRealm = Realm.getInstance(config);

                final RealmResults<PlaceRealm> prevData = backgroundThreadRealm.where(PlaceRealm.class).findAll();

                Log.i(TAG, "BackgroundWrite-run: deleteAllFromRealm");
                backgroundThreadRealm.executeTransaction(transactionRealm -> {
                    prevData.deleteAllFromRealm();
                });

                if (BuildConfig.DEBUG) {
                    RealmResults<PlaceRealm> inDatabase = backgroundThreadRealm.where(PlaceRealm.class).findAll();
                    Log.i(TAG, "BackgroundWrite-run: inDatabase=" + inDatabase.size());
                }

                Log.i(TAG, "BackgroundWrite-run: insert data=" + writeData.size());
                backgroundThreadRealm.executeTransaction(transactionRealm -> {
                    transactionRealm.insert(writeData.values());
                });

                if (BuildConfig.DEBUG) {
                    RealmResults<PlaceRealm> inDatabase = backgroundThreadRealm.where(PlaceRealm.class).findAll();
                    Log.i(TAG, "BackgroundWrite-run: inDatabase=" + inDatabase.size());
                }

                backgroundThreadRealm.close();
                onResultListener.onWriteSuccessfully();
                Log.i(TAG, "BackgroundWrite-run: finish");
            } catch (Exception ex) {
                Log.e(TAG, "BackgroundWrite-run: ex=" + ex);
            }
        }
    }

    public class BackgroundRead implements Runnable {
        @Override
        public void run() {
            Log.i(TAG, "BackgroundRead-run: start");
            try {
                List<PlaceRealm> result = new ArrayList<>();
                RealmConfiguration config = new RealmConfiguration.Builder().name(REALM_NAME).build();
                Realm backgroundThreadRealm = Realm.getInstance(config);
                RealmResults<PlaceRealm> inDatabase = backgroundThreadRealm.where(PlaceRealm.class).findAll();
                Log.i(TAG, "BackgroundRead-run: inDatabase=" + inDatabase.size());
                inDatabase.forEach(new Consumer<PlaceRealm>() {
                    @Override
                    public void accept(PlaceRealm placeRealm) {
                        result.add(new PlaceRealm(placeRealm));
                    }
                });
                onResultListener.onReadResult(result);
                backgroundThreadRealm.close();

                Log.i(TAG, "BackgroundRead-run: finish");
            } catch (Exception ex) {
                Log.e(TAG, "BackgroundRead-run: ex=" + ex);
            }
        }
    }
}

package com.cxb.myfamilytree.presenter;

import android.text.TextUtils;

import com.cxb.myfamilytree.model.FamilyBean;
import com.cxb.myfamilytree.model.FamilyModel;
import com.cxb.myfamilytree.model.IFamilyModel;
import com.cxb.myfamilytree.view.IAddFamilyView;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Action;
import io.reactivex.schedulers.Schedulers;

import static com.cxb.myfamilytree.model.FamilyBean.SEX_FEMALE;
import static com.cxb.myfamilytree.model.FamilyBean.SEX_MALE;

/**
 * 添加亲人Presenter实现
 */

public class AddFamilyPresenter implements IAddFamilyPresenter {

    private IFamilyModel mModel;
    private IAddFamilyView mView;

    public AddFamilyPresenter() {
        mModel = new FamilyModel();
    }

    @Override
    public void addSpouse(FamilyBean selectFamily, FamilyBean addFamily) {
        final String selectFamilySex = selectFamily.getSex();
        final String familySex = addFamily.getSex();
        if (familySex.equals(selectFamilySex)) {
            if (isActive()) {
                mView.showToast("不允许同性配偶");
            }
        } else {
            final String maleId;
            final String femaleId;
            if (SEX_MALE.equals(familySex)) {
                maleId = addFamily.getMemberId();
                femaleId = selectFamily.getMemberId();
            } else {
                maleId = selectFamily.getMemberId();
                femaleId = addFamily.getMemberId();
            }

            Observable
                    .merge(
                            mModel.saveFamily(addFamily),
                            mModel.updateSpouseIdEach(maleId, femaleId),
                            mModel.updateParentId(maleId, femaleId)
                    )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnComplete(new Action() {
                        @Override
                        public void run() throws Exception {
                            if (isActive()) {
                                mView.setResultAndFinish();
                            }
                        }
                    })
                    .subscribe();
        }
    }

    @Override
    public void addParent(FamilyBean selectFamily, FamilyBean addFamily) {
        final String familySex = addFamily.getSex();
        final String fatherId = selectFamily.getFatherId();
        final String motherId = selectFamily.getMotherId();
        final boolean isAddMale = SEX_MALE.equals(familySex);
        if (isAddMale && !TextUtils.isEmpty(fatherId)) {
            if (isActive()) {
                mView.showToast("已有父亲");
            }
        } else if (!isAddMale && !TextUtils.isEmpty(motherId)) {
            if (isActive()) {
                mView.showToast("已有母亲");
            }
        } else {
            final String maleId;
            final String femaleId;
            if (SEX_MALE.equals(familySex)) {
                maleId = addFamily.getMemberId();
                femaleId = motherId;
            } else {
                maleId = fatherId;
                femaleId = addFamily.getMemberId();
            }
            selectFamily.setFatherId(maleId);
            selectFamily.setMotherId(femaleId);
            Observable
                    .merge(
                            mModel.saveFamily(addFamily),
                            mModel.saveFamily(selectFamily),
                            mModel.updateSpouseIdEach(maleId, femaleId),
                            mModel.updateParentId(maleId, femaleId)
                    )
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnComplete(new Action() {
                        @Override
                        public void run() throws Exception {
                            if (isActive()) {
                                mView.setResultAndFinish();
                            }
                        }
                    })
                    .subscribe();
        }
    }

    @Override
    public void addChild(FamilyBean selectFamily, FamilyBean addFamily) {
        final String selectFamilySex = selectFamily.getSex();
        if (SEX_MALE.equals(selectFamilySex)) {
            addFamily.setFatherId(selectFamily.getMemberId());
            addFamily.setMotherId(selectFamily.getSpouseId());
        } else {
            addFamily.setFatherId(selectFamily.getSpouseId());
            addFamily.setMotherId(selectFamily.getMemberId());
        }
        mModel.saveFamily(addFamily)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(new Action() {
                    @Override
                    public void run() throws Exception {
                        if (isActive()) {
                            mView.setResultAndFinish();
                        }
                    }
                })
                .subscribe();
    }

    @Override
    public void addBrothersAndSisters(FamilyBean selectFamily, FamilyBean addFamily) {
        addFamily.setFatherId(selectFamily.getFatherId());
        addFamily.setMotherId(selectFamily.getMotherId());
        mModel.saveFamily(addFamily)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(new Action() {
                    @Override
                    public void run() throws Exception {
                        if (isActive()) {
                            mView.setResultAndFinish();
                        }
                    }
                })
                .subscribe();
    }

    @Override
    public void updateFamilyInfo(FamilyBean family, boolean isChangeGender) {
        final List<ObservableSource> observables = new ArrayList<>();
        observables.add(mModel.saveFamily(family));

        if (isChangeGender) {
            final String familyId = family.getMemberId();
            final String spouseId = family.getSpouseId();
            final String familySex = family.getSex();

            final String maleId;
            final String femaleId;
            final String spouseSex;
            if (SEX_MALE.equals(familySex)) {
                maleId = familyId;
                femaleId = spouseId;
                spouseSex = SEX_FEMALE;
            } else {
                maleId = spouseId;
                femaleId = familyId;
                spouseSex = SEX_MALE;
            }
            observables.add(mModel.updateGender(spouseId, spouseSex));
            observables.add(mModel.exchangeParentId(maleId, femaleId));
        }

        Observable
                .mergeArray(observables.toArray(new ObservableSource[observables.size()]))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnComplete(new Action() {
                    @Override
                    public void run() throws Exception {
                        if (isActive()) {
                            mView.setResultAndFinish();
                        }
                    }
                })
                .subscribe();
    }

    private boolean isActive() {
        return mView != null;
    }

    @Override
    public void attachView(IAddFamilyView view) {
        mView = view;
    }

    @Override
    public void detachView() {
        mView = null;
    }
}
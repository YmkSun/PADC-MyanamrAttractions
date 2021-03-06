package xyz.aungpyaephyo.padc.myanmarattractions.fragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.plus.model.people.Person;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import org.greenrobot.eventbus.EventBus;
import xyz.aungpyaephyo.padc.myanmarattractions.R;
import xyz.aungpyaephyo.padc.myanmarattractions.activities.AccountControlActivity;
import xyz.aungpyaephyo.padc.myanmarattractions.adapters.CountryListAdapter;
import xyz.aungpyaephyo.padc.myanmarattractions.controllers.UserSessionController;
import xyz.aungpyaephyo.padc.myanmarattractions.data.vos.UserVO;
import xyz.aungpyaephyo.padc.myanmarattractions.dialogs.SharedDialog;
import xyz.aungpyaephyo.padc.myanmarattractions.events.DataEvent;
import xyz.aungpyaephyo.padc.myanmarattractions.utils.CommonUtils;
import xyz.aungpyaephyo.padc.myanmarattractions.utils.GAUtils;
import xyz.aungpyaephyo.padc.myanmarattractions.views.PasswordVisibilityListener;

/**
 * Created by aung on 7/15/16.
 */
public class RegisterFragment extends BaseFragment
        implements AccountControlActivity.SocialMediaInfoDelegate {

    private static final int CONNECTED_SOCIAL_MEDIA_FACEBOOK = 1;
    private static final int CONNECTED_SOCIAL_MEDIA_GOOGLE = 2;

    public static final String FRAGMENT_TRANSITION_TAG = "RegisterFragment";

    @BindView(R.id.lbl_registration_title)
    TextView lblRegistrationTitle;

    @BindView(R.id.iv_profile)
    ImageView ivProfile;

    @BindView(R.id.et_name)
    EditText etName;

    @BindView(R.id.et_email)
    EditText etEmail;

    @BindView(R.id.et_password)
    EditText etPassword;

    @BindView(R.id.tv_date_of_birth)
    TextView tvDateOfBirth;

    @BindView(R.id.sp_country_list)
    Spinner spCountryList;

    private CountryListAdapter mCountryListAdapter;
    private UserSessionController mUserSessionController;

    private UserVO mRegisteringUser;

    private int mConnectedSocialMedia;

    public static RegisterFragment newInstance() {
        RegisterFragment fragment = new RegisterFragment();
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mUserSessionController = (UserSessionController) context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String[] countryListArray = getResources().getStringArray(R.array.current_residing_country);
        List<String> countryList = new ArrayList<>(Arrays.asList(countryListArray));

        mCountryListAdapter = new CountryListAdapter(countryList);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_register, container, false);
        ButterKnife.bind(this, rootView);

        lblRegistrationTitle.setText(Html.fromHtml(getString(R.string.lbl_registration_title)));
        spCountryList.setAdapter(mCountryListAdapter);
        etPassword.setOnTouchListener(new PasswordVisibilityListener());

        tvDateOfBirth.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    showDatePicker();
                }
            }
        });

        tvDateOfBirth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDatePicker();
            }
        });

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus eventBus = EventBus.getDefault();
        if (!eventBus.isRegistered(this)) {
            eventBus.register(this);
        }
    }

    @Override
    protected void onSendScreenHit() {
        GAUtils.getInstance().sendScreenHit(GAUtils.SCREEN_REGISTRATION);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus eventBus = EventBus.getDefault();
        eventBus.unregister(this);
    }

    private void showDatePicker() {
        DialogFragment newFragment = new DatePickerDialogFragment();
        newFragment.show(getActivity().getSupportFragmentManager(), "datePicker");
    }

    @OnClick(R.id.btn_register)
    public void onTapRegister(Button btnRegister) {
        String name = etName.getText().toString();
        String email = etEmail.getText().toString();
        String password = etPassword.getText().toString();
        String dateOfBirth = tvDateOfBirth.getText().toString();
        String country = String.valueOf(spCountryList.getSelectedItem());

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password) || TextUtils.isEmpty(dateOfBirth)) {
            //One of the required data is empty
            if (TextUtils.isEmpty(name)) {
                etName.setError(getString(R.string.error_missing_name));
            }
            if (TextUtils.isEmpty(email)) {
                etEmail.setError(getString(R.string.error_missing_email));
            }

            if (TextUtils.isEmpty(password)) {
                etPassword.setError(getString(R.string.error_missing_password));
            }

            if (TextUtils.isEmpty(dateOfBirth)) {
                tvDateOfBirth.setError(getString(R.string.error_missing_date_of_birth));
            }
        } else if (!CommonUtils.isEmailValid(email)) {
            //Email address is not in the right format.
            etEmail.setError(getString(R.string.error_email_is_not_valid));
        } else {
            //Checking on client side is done here.
            if (mRegisteringUser == null) { //Regular Registration
                mUserSessionController.onRegister(name, email, password, dateOfBirth, country);
            } else { //Registration with Social Media.
                mRegisteringUser.setDateOfBirthText(dateOfBirth);
                mRegisteringUser.setCountryOfOrigin(country);

                if (mConnectedSocialMedia == CONNECTED_SOCIAL_MEDIA_FACEBOOK) {
                    mUserSessionController.onRegisterWithFacebook(mRegisteringUser, password);
                } else if (mConnectedSocialMedia == CONNECTED_SOCIAL_MEDIA_GOOGLE) {
                    mUserSessionController.onRegisterWithGoogle(mRegisteringUser, password);
                }
            }
        }
    }

    @OnClick(R.id.iv_register_with_facebook)
    public void onTapRegisterWithFacebook(View view) {
        mUserSessionController.connectToFacebook(this);
    }

    @OnClick(R.id.iv_register_with_google)
    public void onTapRegisterWithGoogle(View view) {
        mUserSessionController.connectToGoogle(this);
    }

    private void showRetrievedDataInRegistrationForm(UserVO registeringUser) {
        if (!TextUtils.isEmpty(registeringUser.getName())) {
            etName.setText(registeringUser.getName());
        }

        if (!TextUtils.isEmpty(registeringUser.getEmail())) {
            etEmail.setText(registeringUser.getEmail());
        }

        if(!TextUtils.isEmpty(registeringUser.getProfilePicture())) {
            ivProfile.setVisibility(View.VISIBLE);
            Glide.with(getContext())
                    .load(registeringUser.getProfilePicture())
                    .asBitmap().centerCrop()
                    .placeholder(R.drawable.dummy_avatar)
                    .error(R.drawable.dummy_avatar)
                    .into(new BitmapImageViewTarget(ivProfile) {
                        @Override
                        protected void setResource(Bitmap resource) {
                            RoundedBitmapDrawable circularBitmapDrawable =
                                    RoundedBitmapDrawableFactory.create(ivProfile.getResources(), resource);
                            circularBitmapDrawable.setCircular(true);
                            ivProfile.setImageDrawable(circularBitmapDrawable);
                        }
                    });
        } else {
            ivProfile.setVisibility(View.GONE);
        }

        etPassword.requestFocus();

        SharedDialog.promptMsgWithTheme(getActivity(),
                getString(R.string.prompt_some_data_retrieved_for_registration));
    }

    public void onRetrieveGoogleInfo(GoogleSignInAccount signInAccount, Person registeringUser) {
        mConnectedSocialMedia = CONNECTED_SOCIAL_MEDIA_GOOGLE;
        mRegisteringUser = UserVO.initFromGoogleInfo(signInAccount, registeringUser);

        showRetrievedDataInRegistrationForm(mRegisteringUser);
    }

    public void onRetrieveFacebookInfo(JSONObject facebookLoginUser, String imageUrl, String coverImageUrl) {
        mConnectedSocialMedia = CONNECTED_SOCIAL_MEDIA_FACEBOOK;
        mRegisteringUser = UserVO.initFromFacebookInfo(facebookLoginUser, imageUrl, coverImageUrl);

        showRetrievedDataInRegistrationForm(mRegisteringUser);
    }

    @Override
    public boolean isRegistering() {
        return true;
    }

    //Success Register
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(DataEvent.DatePickedEvent event) {
        tvDateOfBirth.setText(event.getDateOfBrith());
    }
}

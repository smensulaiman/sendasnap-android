package com.sendajapan.sendasnap.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.sendajapan.sendasnap.R;
import com.sendajapan.sendasnap.activities.HistoryActivity;
import com.sendajapan.sendasnap.databinding.FragmentProfileBinding;
import com.sendajapan.sendasnap.models.UserData;
import com.sendajapan.sendasnap.models.Vendor;
import com.sendajapan.sendasnap.utils.HapticFeedbackHelper;
import com.sendajapan.sendasnap.utils.SharedPrefsManager;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private SharedPrefsManager prefsManager;
    private HapticFeedbackHelper hapticHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initHelpers();
        setupClickListeners();
        loadUserData();
    }

    private void initHelpers() {
        prefsManager = SharedPrefsManager.getInstance(requireContext());
        hapticHelper = HapticFeedbackHelper.getInstance(requireContext());
    }

    private void setupClickListeners() {
        binding.switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            hapticHelper.vibrateClick();
            prefsManager.setNotificationsEnabled(isChecked);
        });

        binding.switchHaptic.setOnCheckedChangeListener((buttonView, isChecked) -> {
            hapticHelper.vibrateClick();
            prefsManager.setHapticEnabled(isChecked);
        });

        binding.layoutHistory.setOnClickListener(v -> {
            hapticHelper.vibrateClick();
            openHistory();
        });
    }

    private void loadUserData() {
        UserData user = prefsManager.getUser();
        Vendor vendor = prefsManager.getVendor();

        if (user != null) {
            String userName = user.getName();
            String userEmail = user.getEmail();
            String userRole = user.getRole();

            if (userName != null && !userName.isEmpty()) {
                binding.txtUserName.setText(userName);
                binding.txtFullName.setText(userName);
            } else {
                binding.txtUserName.setText("User");
                binding.txtFullName.setText("User");
            }

            if (userEmail != null && !userEmail.isEmpty()) {
                binding.txtUserEmail.setText(userEmail);
                binding.txtEmailAddress.setText(userEmail);
            } else {
                String email = prefsManager.getEmail();
                if (email != null && !email.isEmpty()) {
                    binding.txtUserEmail.setText(email);
                    binding.txtEmailAddress.setText(email);
                }
            }

            if (userRole != null && !userRole.isEmpty()) {
                String capitalizedRole = userRole.substring(0, 1).toUpperCase() +
                        userRole.substring(1).toLowerCase();
                binding.txtRole.setText(capitalizedRole);
                binding.txtUserRole.setText(capitalizedRole);
            } else {
                binding.txtRole.setText("User");
                binding.txtUserRole.setText("User");
            }

            loadProfilePicture(user);
        } else {
            String username = prefsManager.getUsername();
            String email = prefsManager.getEmail();

            if (username != null && !username.isEmpty()) {
                binding.txtUserName.setText(username);
                binding.txtFullName.setText(username);
            } else {
                binding.txtUserName.setText("User");
                binding.txtFullName.setText("User");
            }

            if (email != null && !email.isEmpty()) {
                binding.txtUserEmail.setText(email);
                binding.txtEmailAddress.setText(email);
            }

            binding.txtRole.setText("User");
            binding.txtUserRole.setText("User");
        }

        if (user != null) {
            loadProfilePicture(user);
        } else {
            binding.imgProfile.setImageResource(R.drawable.avater_placeholder);
        }

        if (vendor != null) {
            if (binding.txtCompanyName != null) {
                String companyName = vendor.getName();
                if (companyName != null && !companyName.isEmpty()) {
                    binding.txtCompanyName.setText(companyName);
                } else {
                    binding.txtCompanyName.setText("N/A");
                }
            }

            if (binding.txtCompanyAddress != null) {
                String address = buildAddressString(vendor);
                if (address != null && !address.isEmpty()) {
                    binding.txtCompanyAddress.setText(address);
                } else {
                    binding.txtCompanyAddress.setText("N/A");
                }
            }
        } else {
            if (binding.txtCompanyName != null) {
                binding.txtCompanyName.setText("N/A");
            }
            if (binding.txtCompanyAddress != null) {
                binding.txtCompanyAddress.setText("N/A");
            }
        }

        binding.switchNotifications.setChecked(prefsManager.isNotificationsEnabled());
        binding.switchHaptic.setChecked(prefsManager.isHapticEnabled());
    }

    private String buildAddressString(Vendor vendor) {
        if (vendor == null) {
            return null;
        }

        StringBuilder addressBuilder = new StringBuilder();
        if (vendor.getAddress() != null && !vendor.getAddress().isEmpty()) {
            addressBuilder.append(vendor.getAddress());
        }
        if (vendor.getCity() != null && !vendor.getCity().isEmpty()) {
            if (addressBuilder.length() > 0) {
                addressBuilder.append(", ");
            }
            addressBuilder.append(vendor.getCity());
        }
        if (vendor.getState() != null && !vendor.getState().isEmpty()) {
            if (addressBuilder.length() > 0) {
                addressBuilder.append(", ");
            }
            addressBuilder.append(vendor.getState());
        }
        if (vendor.getCountry() != null && !vendor.getCountry().isEmpty()) {
            if (addressBuilder.length() > 0) {
                addressBuilder.append(", ");
            }
            addressBuilder.append(vendor.getCountry());
        }
        if (vendor.getZip() != null && !vendor.getZip().isEmpty()) {
            if (addressBuilder.length() > 0) {
                addressBuilder.append(" ");
            }
            addressBuilder.append(vendor.getZip());
        }

        return addressBuilder.length() > 0 ? addressBuilder.toString() : null;
    }

    private void loadProfilePicture(UserData user) {
        if (binding.imgProfile == null || user == null) {
            return;
        }

        String avatarUrl = user.getAvatarUrl() != null ? user.getAvatarUrl() : user.getAvatar();

        if (avatarUrl != null && !avatarUrl.isEmpty() && isValidUrl(avatarUrl)) {
            Glide.with(requireContext())
                    .load(avatarUrl)
                    .placeholder(R.drawable.avater_placeholder)
                    .error(R.drawable.avater_placeholder)
                    .circleCrop()
                    .into(binding.imgProfile);
        } else {
            binding.imgProfile.setImageResource(R.drawable.avater_placeholder);
        }
    }

    private boolean isValidUrl(String url) {
        return url != null && (url.startsWith("http://") || url.startsWith("https://"));
    }

    private void openHistory() {
        Intent intent = new Intent(requireContext(), HistoryActivity.class);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

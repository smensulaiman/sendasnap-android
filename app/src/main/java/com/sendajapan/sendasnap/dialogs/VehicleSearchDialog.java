package com.sendajapan.sendasnap.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.sendajapan.sendasnap.R;
import com.sendajapan.sendasnap.utils.HapticFeedbackHelper;

import java.util.ArrayList;
import java.util.List;

public class VehicleSearchDialog {

    public interface OnSearchListener {
        void onSearch(String searchType, String searchQuery, String company);
        void onYardSearch(String yardId, String company, String yardName);
    }

    private static class YardItem {
        static final int TYPE_HEADER = 0;
        static final int TYPE_YARD = 1;
        final int type;
        final String displayName;
        final String yardId;
        final String company;

        YardItem(int type, String displayName, String yardId, String company) {
            this.type = type;
            this.displayName = displayName;
            this.yardId = yardId;
            this.company = company;
        }
    }

    private static class YardSpinnerAdapter extends BaseAdapter {

        private final Context context;
        private final List<YardItem> items;

        YardSpinnerAdapter(Context context) {
            this.context = context;
            items = new ArrayList<>();
            items.add(new YardItem(YardItem.TYPE_HEADER, "AUTOCRAFT JAPAN LTD", null, null));
            items.add(new YardItem(YardItem.TYPE_YARD, "MENZAI SUGAYA", "437", "acjl"));
            items.add(new YardItem(YardItem.TYPE_YARD, "HANYA YARD", "1861", "acjl"));
            items.add(new YardItem(YardItem.TYPE_YARD, "ACJ KOBE", "2385", "acjl"));
            items.add(new YardItem(YardItem.TYPE_HEADER, "KARMEN", null, null));
            items.add(new YardItem(YardItem.TYPE_YARD, "Sugaya Yard", "805", "karmen"));
            items.add(new YardItem(YardItem.TYPE_YARD, "Hanya yard", "1384", "karmen"));
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public YardItem getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean isEnabled(int position) {
            return items.get(position).type == YardItem.TYPE_YARD;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView textView;
            if (convertView == null) {
                textView = (TextView) LayoutInflater.from(context).inflate(
                        R.layout.spinner_item_search_type, parent, false);
            } else {
                textView = (TextView) convertView;
            }
            textView.setText(items.get(position).displayName);
            return textView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            YardItem item = items.get(position);
            if (item.type == YardItem.TYPE_HEADER) {
                if (convertView == null || !Boolean.TRUE.equals(convertView.getTag())) {
                    convertView = LayoutInflater.from(context).inflate(
                            R.layout.spinner_dropdown_item_yard_header, parent, false);
                    convertView.setTag(Boolean.TRUE);
                }
            } else {
                if (convertView == null || Boolean.TRUE.equals(convertView.getTag())) {
                    convertView = LayoutInflater.from(context).inflate(
                            R.layout.spinner_dropdown_item_bordered, parent, false);
                    convertView.setTag(Boolean.FALSE);
                }
            }
            ((TextView) convertView).setText(item.displayName);
            return convertView;
        }
    }

    public static class Builder {

        private final Context context;
        private OnSearchListener searchListener;
        private boolean cancelable = true;
        private final HapticFeedbackHelper hapticHelper;

        public Builder(@NonNull Context context) {
            this.context = context;
            this.hapticHelper = HapticFeedbackHelper.getInstance(context);
        }

        public Builder setOnSearchListener(OnSearchListener listener) {
            this.searchListener = listener;
            return this;
        }

        public Builder setCancelable(boolean cancelable) {
            this.cancelable = cancelable;
            return this;
        }

        public Dialog create() {
            Dialog dialog = new Dialog(context);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_vehicle_search);
            dialog.setCancelable(cancelable);

            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                dialog.getWindow().setBackgroundDrawableResource(R.drawable.outline_box_shape);
            }

            TabLayout tabLayout = dialog.findViewById(R.id.tabLayout);
            LinearLayout layoutSearchContent = dialog.findViewById(R.id.layoutSearchContent);
            LinearLayout layoutYardContent = dialog.findViewById(R.id.layoutYardContent);

            Spinner spinnerCompany = dialog.findViewById(R.id.spinnerCompany);
            ImageView imgCompanySpinnerArrow = dialog.findViewById(R.id.imgCompanySpinnerArrow);

            Spinner spinnerSearchType = dialog.findViewById(R.id.spinnerSearchType);
            ImageView imgSpinnerArrow = dialog.findViewById(R.id.imgSpinnerArrow);
            TextInputLayout tilSearchQuery = dialog.findViewById(R.id.tilSearchQuery);
            TextInputEditText etSearchQuery = dialog.findViewById(R.id.etSearchQuery);

            Spinner spinnerYard = dialog.findViewById(R.id.spinnerYard);
            ImageView imgYardSpinnerArrow = dialog.findViewById(R.id.imgYardSpinnerArrow);

            MaterialButton btnAction = dialog.findViewById(R.id.btnAction);
            MaterialButton btnCancel = dialog.findViewById(R.id.btnCancel);

            tabLayout.addTab(tabLayout.newTab().setText("Search"));
            tabLayout.addTab(tabLayout.newTab().setText("Yard"));

            String[] companies = {"Autocraft Japan Ltd", "Karmen"};
            String[] companyValues = {"acjl", "karmen"};

            ArrayAdapter<String> companyAdapter = new ArrayAdapter<>(context,
                    R.layout.spinner_item_search_type, companies);
            companyAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_bordered);
            spinnerCompany.setAdapter(companyAdapter);
            spinnerCompany.setSelection(0);

            final int[] selectedCompanyIndex = {0};
            spinnerCompany.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectedCompanyIndex[0] = position;
                    hapticHelper.vibrateClick();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            imgCompanySpinnerArrow.setOnClickListener(v -> {
                spinnerCompany.performClick();
                hapticHelper.vibrateClick();
            });

            String[] searchTypes = {"Vehicle ID", "Chassis Number"};
            String[] searchTypeValues = {"vehicle_id", "veh_chassis_number"};

            ArrayAdapter<String> searchAdapter = new ArrayAdapter<>(context,
                    R.layout.spinner_item_search_type, searchTypes);
            searchAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_bordered);
            spinnerSearchType.setAdapter(searchAdapter);
            spinnerSearchType.setSelection(0);

            final int[] selectedSearchIndex = {0};
            spinnerSearchType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    selectedSearchIndex[0] = position;
                    hapticHelper.vibrateClick();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            imgSpinnerArrow.setOnClickListener(v -> {
                spinnerSearchType.performClick();
                hapticHelper.vibrateClick();
            });

            YardSpinnerAdapter yardAdapter = new YardSpinnerAdapter(context);
            spinnerYard.setAdapter(yardAdapter);
            spinnerYard.setSelection(1);

            final int[] selectedYardPosition = {1};
            spinnerYard.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (yardAdapter.getItem(position).type == YardItem.TYPE_HEADER) {
                        spinnerYard.setSelection(selectedYardPosition[0]);
                    } else {
                        selectedYardPosition[0] = position;
                        hapticHelper.vibrateClick();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {}
            });

            imgYardSpinnerArrow.setOnClickListener(v -> {
                spinnerYard.performClick();
                hapticHelper.vibrateClick();
            });

            final boolean[] isYardTabActive = {false};
            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    boolean isYard = tab.getPosition() == 1;
                    isYardTabActive[0] = isYard;
                    layoutSearchContent.setVisibility(isYard ? View.GONE : View.VISIBLE);
                    layoutYardContent.setVisibility(isYard ? View.VISIBLE : View.GONE);
                    btnAction.setText(isYard ? "Show Vehicles" : context.getString(R.string.search));
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {}

                @Override
                public void onTabReselected(TabLayout.Tab tab) {}
            });

            btnCancel.setOnClickListener(v -> {
                hapticHelper.vibrateClick();
                dialog.dismiss();
            });

            btnAction.setOnClickListener(v -> {
                if (isYardTabActive[0]) {
                    hapticHelper.vibrateClick();
                    dialog.dismiss();
                    if (searchListener != null) {
                        YardItem selectedYard = yardAdapter.getItem(selectedYardPosition[0]);
                        searchListener.onYardSearch(selectedYard.yardId, selectedYard.company, selectedYard.displayName);
                    }
                } else {
                    String searchQuery = etSearchQuery.getText().toString().trim();
                    tilSearchQuery.setError(null);

                    if (searchQuery.isEmpty()) {
                        tilSearchQuery.setError("Search query is required");
                        etSearchQuery.requestFocus();
                        hapticHelper.vibrateError();
                        return;
                    }

                    if (searchQuery.length() < 3) {
                        tilSearchQuery.setError("Search query must be at least 3 characters");
                        etSearchQuery.requestFocus();
                        hapticHelper.vibrateError();
                        return;
                    }

                    hapticHelper.vibrateClick();
                    dialog.dismiss();

                    if (searchListener != null) {
                        searchListener.onSearch(searchTypeValues[selectedSearchIndex[0]], searchQuery,
                                companyValues[selectedCompanyIndex[0]]);
                    }
                }
            });

            return dialog;
        }

        public void show() {
            create().show();
        }
    }
}

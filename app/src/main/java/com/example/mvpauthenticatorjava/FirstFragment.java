package com.example.mvpauthenticatorjava;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.mvpauthenticatorjava.databinding.FragmentFirstBinding;
import com.example.mvpauthenticatorjava.service.ExternalReceiver;
import com.google.android.material.snackbar.Snackbar;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import org.json.JSONException;
import org.json.JSONObject;

public class FirstFragment extends Fragment {

    public static final String TAG = FirstFragment.class.getSimpleName();

    private FragmentFirstBinding binding;

    private String jsonResult = "";

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter(ExternalReceiver.ACTION_MVP_RESULT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireActivity().registerReceiver(internalReceiver, intentFilter, Context.RECEIVER_EXPORTED);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        requireActivity().unregisterReceiver(internalReceiver);
        Log.d(TAG, "MVP Result Receiver unregistered.");
    }


    /**
     * BroadcastReceiver to receive the result of the MVP verification from MvpResultReceiver.
     */
    private final BroadcastReceiver internalReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && ExternalReceiver.ACTION_MVP_RESULT.equals(intent.getAction())) {
                String result = intent.getStringExtra(ExternalReceiver.RESULT);
                if (result == null) {
                    result = "No result received";
                }
                binding.textviewFirst.setText(result);
                Snackbar.make(binding.getRoot(), result, Snackbar.LENGTH_LONG).show();
            }
        }
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.scannerButton.setOnClickListener(v -> startScanner());

        binding.checkMvpAppButton.setOnClickListener(v -> {
            if (MVPVerificationService.checkMvpAppInstalled(requireActivity())) {
                Snackbar.make(binding.getRoot(), "MVP installed!", Snackbar.LENGTH_SHORT).show();
            } else {
                Snackbar.make(binding.getRoot(), "MVP not installed!", Snackbar.LENGTH_SHORT).show();
            }
        });

        binding.authenticateButton.setOnClickListener(v -> {
            if (jsonResult == null || jsonResult.trim().isEmpty()) {
                Log.w(TAG, "Authenticate button clicked but no QR code was scanned.");
                Snackbar.make(binding.getRoot(), "Please scan a QR code first.", Snackbar.LENGTH_SHORT).show();
                return; // Exit the listener
            }

            try {
                JSONObject scannedJson = new JSONObject(jsonResult);

                String token = scannedJson.optString("token", "");

                Log.d(TAG, "Starting background verification process.");
                Snackbar.make(binding.getRoot(), "Verification sent to background...", Snackbar.LENGTH_SHORT).show();

                if (!token.isEmpty()) {
                    Log.d(TAG, "Authenticating with token.");
                    MVPVerificationService.authenticate(v.getContext(), token);
                } else {
                    String accountNumber = scannedJson.optString("accountNumber", "");
                    String code = scannedJson.optString("code", "");
                    String codeType = scannedJson.optString("codeType", "");
                    String imoNumber = scannedJson.optString("imoNumber", "");
                    String licenseNumber = scannedJson.optString("licenseNumber", "");

                    String authenticationCodeValue;

                    if ("imoNumber".equals(codeType)) {
                        authenticationCodeValue = imoNumber + accountNumber;
                    } else if ("licenseNumber".equals(codeType)) {
                        authenticationCodeValue = licenseNumber + accountNumber;
                    } else {
                        Log.w(TAG, "Unknown codeType in QR code: " + codeType);
                        Snackbar.make(binding.getRoot(), "QR code has an unsupported code type.", Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    if (authenticationCodeValue.isEmpty() || code.isEmpty()) {
                        Log.w(TAG, "Missing required values for code-based authentication.");
                        Snackbar.make(binding.getRoot(), "Incomplete QR code data for this verification type.", Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    Log.d(TAG, "Authenticating with code: " + authenticationCodeValue);
                    MVPVerificationService.authenticate(v.getContext(), authenticationCodeValue, code);
                }

                // The result will come back to ExternalReceiver -> broadcast to UI -> internalReceiver automatically.
                binding.textviewFirst.setText("Token sent. Waiting for background result...");
            } catch (JSONException e) {
                Log.e(TAG, "Scanned QR code content is not valid JSON.", e);
                Snackbar.make(binding.getRoot(), "Invalid QR code format.", Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void startScanner() {
        ScanOptions options = new ScanOptions();
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        options.setPrompt("Scan a QR code");
        options.setCameraId(0);
        options.setBeepEnabled(false);
        options.setBarcodeImageEnabled(true);
        barcodeLauncher.launch(options);
    }

    private final ActivityResultLauncher<ScanOptions> barcodeLauncher = registerForActivityResult(new ScanContract(),
            result -> {
                if (result.getContents() == null) {
                    Snackbar.make(binding.getRoot(), "Cancelled", Snackbar.LENGTH_LONG).show();
                } else {
                    Snackbar.make(binding.getRoot(), "Scanned", Snackbar.LENGTH_LONG).show();
                    binding.textviewFirst.setText(result.getContents());
                    jsonResult = result.getContents();
                }
            });
}
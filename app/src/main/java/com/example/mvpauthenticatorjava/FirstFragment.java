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
import com.example.mvpauthenticatorjava.service.MyService;
import com.google.android.material.snackbar.Snackbar;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

import org.json.JSONException;
import org.json.JSONObject;

public class FirstFragment extends Fragment {

    public static final String TAG = FirstFragment.class.getSimpleName();

    private FragmentFirstBinding binding;

    private String jsonResult = "";

    /**
     * BroadcastReceiver to receive the result of the MVP verification.
     */
    private final BroadcastReceiver mvpResultReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Check if the received broadcast has the correct action
            if (intent != null && MyService.ACTION_MVP_RESULT.equals(intent.getAction())) {
                String status = intent.getStringExtra(MyService.EXTRA_STATUS);
                if (status == null) {
                    status = "No status received";
                }
                String message = "Result from MVP App: " + status;

                Log.d(TAG, message);

                // Update the UI with the final result
                binding.textviewFirst.setText(message);
                Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
            }
        }
    };

    // ActivityResultLauncher for the barcode scanner
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

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment using View Binding
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        return binding.getRoot();
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
            // 1. --- Validation ---
            // Ensure a QR code has been scanned first.
            if (jsonResult == null || jsonResult.trim().isEmpty()) {
                Log.w(TAG, "Authenticate button clicked but no QR code was scanned.");
                Snackbar.make(binding.getRoot(), "Please scan a QR code first.", Snackbar.LENGTH_SHORT).show();
                return; // Exit the listener
            }

            try {
                // 2. --- JSON Parsing ---
                JSONObject scannedJson = new JSONObject(jsonResult);

                // 3. --- Logic Branching: Decide whether to use Token or Code ---
                String token = scannedJson.optString("token", "");

                Log.d(TAG, "Starting background verification process.");
                Snackbar.make(binding.getRoot(), "Verification sent to background...", Snackbar.LENGTH_SHORT).show();

                if (token != null && !token.isEmpty()) {
                    // --- A. Authenticate using the provided token ---
                    Log.d(TAG, "Authenticating with token.");
                    // Assuming an overloaded method or a specific one for token
                    MVPVerificationService.authenticate(v.getContext(), token);

                    // The result will come back to the BroadcastReceiver automatically.
                    binding.textviewFirst.setText("Token sent. Waiting for background result...");
                } else {
                    // --- B. Authenticate using IMO/License number and code ---
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
                        // Handle unknown codeType and exit.
                        Log.w(TAG, "Unknown codeType in QR code: " + codeType);
                        Snackbar.make(binding.getRoot(), "QR code has an unsupported code type.", Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    // Check if the required values were found.
                    if (authenticationCodeValue.isEmpty() || code.isEmpty()) {
                        Log.w(TAG, "Missing required values for code-based authentication.");
                        Snackbar.make(binding.getRoot(), "Incomplete QR code data for this verification type.", Snackbar.LENGTH_LONG).show();
                        return;
                    }

                    Log.d(TAG, "Authenticating with code: " + authenticationCodeValue);
                    MVPVerificationService.authenticate(v.getContext(), authenticationCodeValue, code);

                    // The result will come back to the BroadcastReceiver automatically.
                    binding.textviewFirst.setText("Code sent. Waiting for background result...");
                }

            } catch (JSONException e) {
                // This block will run if the scanned text is not valid JSON
                Log.e(TAG, "Scanned QR code content is not valid JSON.", e);
                Snackbar.make(binding.getRoot(), "Invalid QR code format.", Snackbar.LENGTH_LONG).show();
            }
        });

    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter intentFilter = new IntentFilter(MyService.ACTION_MVP_RESULT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireActivity().registerReceiver(mvpResultReceiver, intentFilter, Context.RECEIVER_EXPORTED);
        } else {
            requireActivity().registerReceiver(mvpResultReceiver, intentFilter);
        }
        Log.d(TAG, "MVP Result Receiver registered.");
    }

    @Override
    public void onStop() {
        super.onStop();
        requireActivity().unregisterReceiver(mvpResultReceiver);
        Log.d(TAG, "MVP Result Receiver unregistered.");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Nullify the binding object to prevent memory leaks
        binding = null;
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
}
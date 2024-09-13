package com.example.tt;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.mylibrary.AESEncryptor;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'tt' library on application startup.

    private static final String INPUT_KEY = "vietnam";
    private static final byte[] AES_KEY = generateKey(INPUT_KEY);
    private static final byte[] INIT_VECTOR = generateInitVector();
    private static final int PICK_TXT_FILE = 1;
    private static final int CREATE_FILE = 2;
    private static final String GIAI_MA = "Giải mã";
    private static final String MA_HOA = "Mã hóa";
    private static final String HEADER_FILE = "ENCRYPTED_FILE";
    private static boolean IS_EN_F = false;
    byte[] encryptedText;

    private WebView webView;
    private Button selectFileButton;
    private Button saveFileButton;
    private String fileContent = "";
    private byte[] fileContentByte;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        selectFileButton = findViewById(R.id.button_select_file);
        saveFileButton = findViewById(R.id.button_save_file);

        // Cấu hình WebView cho phép cuộn ngang
        webView.setHorizontalScrollBarEnabled(true);
        WebSettings webSettings = webView.getSettings();
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);
        webView.setInitialScale(200);

        // Khi bấm nút, mở bộ chọn file
        selectFileButton.setOnClickListener(v -> openFilePicker());

        saveFileButton.setOnClickListener(v -> showSaveDialog());
    }

    private void openFilePicker() {
        // Mở bộ chọn file
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/plain");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_TXT_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_TXT_FILE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                // Đọc nội dung của file được chọn và hiển thị trong WebView
//                readTextFile(uri);
                readBinaryFile(uri);

//                Toast.makeText(this, encryptedText, Toast.LENGTH_SHORT).show();
                // Hiển thị nút lưu file sau khi chọn file thành công
//                if (isEncryptedFile(fileContent.getBytes())) {
//                    saveFileButton.setText("Giải mã");
//                } else {
//                    saveFileButton.setText("Mã hóa");
//                }
                if (IS_EN_F) {
                    saveFileButton.setText("Giải mã");
                } else {
                    saveFileButton.setText("Mã hóa");
                }

                saveFileButton.setVisibility(Button.VISIBLE);

            }
        } else if (requestCode == CREATE_FILE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                if (!IS_EN_F) {
                    encryptedText = AESEncryptor.getInstance().encrypt(fileContentByte, AES_KEY, INIT_VECTOR);
//                    saveTextFile(uri, concatenate(HEADER_FILE.getBytes(), encryptedText));
                    saveTextFile(uri, encryptedText);
                    IS_EN_F = true;
                } else {
//                    byte[] input = removeHeader(fileContent.getBytes());
                    byte[] d = fileContent.getBytes();
                    byte[] tt = AESEncryptor.getInstance().decrypt(fileContentByte, AES_KEY, INIT_VECTOR);
                    saveTextFile(uri, tt);
                    IS_EN_F = false;
                }


                // Sau khi lưu xong, ẩn nút lưu và xóa nội dung WebView
                saveFileButton.setVisibility(Button.GONE);
                webView.loadData("", "text/html", "UTF-8"); // Xóa nội dung WebView
            }
        }
    }

    // Hiển thị dialog để người dùng nhập tên file mới

    private void showSaveDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nhập tên file mới");

        final EditText input = new EditText(this);
        input.setHint("Tên file");
        builder.setView(input);

        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String fileName = input.getText().toString();
            if (!fileName.endsWith(".txt")) {
                fileName += ".txt";
            }
            createNewFile(fileName);
        });
        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    // Mở bộ chọn vị trí lưu file
    private void createNewFile(String fileName) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        startActivityForResult(intent, CREATE_FILE);
    }


    private void readBinaryFile(Uri uri) {
        try {
            // Mở InputStream để đọc tệp nhị phân
            InputStream inputStream = getContentResolver().openInputStream(uri);

            // Đọc toàn bộ nội dung nhị phân vào một mảng byte
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];

            int len = 0;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }

            fileContentByte = byteBuffer.toByteArray(); // Dữ liệu nhị phân

            inputStream.close();



            // Ép kiểu nhị phân thành chuỗi ký tự (nếu dữ liệu là văn bản)
            String fileContent = new String(fileContentByte, StandardCharsets.UTF_8); // Ép kiểu thành chuỗi UTF-8

//            // Xử lý theo điều kiện của bạn
//            if (fileContent.contains(HEADER_FILE)) {
//                // Nếu có HEADER_FILE, dùng toàn bộ nội dung file
//                fileContent = new String(binaryData, StandardCharsets.UTF_8);
//            } else {
//                // Nếu không có HEADER_FILE, giữ lại nội dung như văn bản
//                fileContent = fileContent.replace("\n", "");
//            }

            // Định dạng lại nội dung để hiển thị trong WebView
            String formattedText = "<pre>" + fileContent + "</pre>";
            webView.loadDataWithBaseURL(null, formattedText, "text/html", "UTF-8", null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void readTextFile(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            StringBuilder stringBuilder = new StringBuilder();
            StringBuilder d = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                d.append(line);
                stringBuilder.append(line);
            }
            reader.close();

            if (d.toString().contains(HEADER_FILE)) {
                fileContent = d.toString();
            } else {
                fileContent = stringBuilder.toString();
            }
            String formattedText = "<pre>" + fileContent + "</pre>";
            // Hiển thị nội dung trong WebView
            webView.loadDataWithBaseURL(null, formattedText, "text/html", "UTF-8", null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Lưu nội dung vào file mới
    private void saveTextFile(Uri uri, byte[] data) {
        try {
            OutputStream outputStream = getContentResolver().openOutputStream(uri);
            if (outputStream != null) {
                outputStream.write(data);
                outputStream.close();
                Toast.makeText(this, "File đã được lưu", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lưu file thất bại", Toast.LENGTH_SHORT).show();
        }
    }


    public static byte[] removeHeader(byte[] data) {
        // Xác định độ dài của header
        int headerLength = HEADER_FILE.getBytes().length;

        // Tạo mảng mới để lưu phần dữ liệu đã được mã hóa (sau header)
        byte[] encryptedData = new byte[data.length - headerLength];

        // Sao chép dữ liệu sau header vào mảng encryptedData
        System.arraycopy(data, headerLength, encryptedData, 0, encryptedData.length);

        return encryptedData;
    }

    public static byte[] generateKey(String input) {
        try {
            // Sử dụng SHA-256 để băm chuỗi đầu vào "vietnam"
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] key = sha.digest(input.getBytes()); // Băm chuỗi thành mảng byte 32-byte (256-bit)

            // Trả về mảng byte 32-byte dùng làm khóa AES-256
            return key;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] generateInitVector() {
        SecureRandom random = new SecureRandom();
        byte[] key = new byte[32]; // 256 bit
        random.nextBytes(key);
        return key;
    }

    public static byte[] concatenate(byte[] array1, byte[] array2) {
        byte[] result = new byte[array1.length + array2.length];
        System.arraycopy(array1, 0, result, 0, array1.length);
        System.arraycopy(array2, 0, result, array1.length, array2.length);
        return result;
    }

    public static boolean isEncryptedFile(byte[] data) {
        // Xác định header bạn đã thêm vào (ví dụ "ENCRYPTED_FILE")

        byte[] headerBytes = HEADER_FILE.getBytes();

        // Kiểm tra xem độ dài dữ liệu có đủ để chứa header không
        if (data.length < headerBytes.length) {
            return false; // Dữ liệu quá ngắn, không thể chứa header
        }

        // So sánh header của dữ liệu với header dự kiến
        for (int i = 0; i < headerBytes.length; i++) {
            if (data[i] != headerBytes[i]) {
                return false; // Header không khớp
            }
        }

        return true; // Header khớp, dữ liệu có thể là tệp mã hóa
    }
}
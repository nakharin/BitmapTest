package com.bxl.bitmaptest;

import java.util.ArrayList;
import java.util.Set;

import com.bxl.config.editor.BXLConfigLoader;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import jpos.JposException;
import jpos.SmartCardRW;
import jpos.SmartCardRWConst;
import jpos.config.JposEntry;
import jpos.events.DataEvent;
import jpos.events.DataListener;
import jpos.events.ErrorEvent;
import jpos.events.ErrorListener;
import jpos.events.OutputCompleteEvent;
import jpos.events.OutputCompleteListener;
import jpos.events.StatusUpdateEvent;
import jpos.events.StatusUpdateListener;


public class MainActivity extends Activity implements
        View.OnClickListener, AdapterView.OnItemClickListener,
        SeekBar.OnSeekBarChangeListener, OnCheckedChangeListener, ErrorListener, StatusUpdateListener, OutputCompleteListener, DataListener {

    private static final String TAG = "MainActivity";

    private static final int REQUEST_CODE_BLUETOOTH = 1;
    private static final int REQUEST_CODE_ACTION_PICK = 2;

    private static final String DEVICE_ADDRESS_START = " (";
    private static final String DEVICE_ADDRESS_END = ")";

    private final ArrayList<CharSequence> bondedDevices = new ArrayList<>();
    private ArrayAdapter<CharSequence> arrayAdapter;

    private ListView listView;

    private TextView pathTextView;
    private TextView progressTextView;

    private RadioGroup openRadioGroup;

    private Button openFromDeviceStorageButton;
    private Button buttonOpenPrinter;
    private Button buttonPrint;
    private Button buttonClosePrinter;

    private SeekBar seekBar;

    private BXLConfigLoader bxlConfigLoader;

    /**
     * Printer
     */
//    private POSPrinter posPrinter;

    /**
     * MSR
     */
    // 	private MSR msr;

    /**
     * Smart Card
     */
    private SmartCardRW smartCardRW;

    private String logicalName;
    private int brightness = 50;
    private int compress = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buildView();

        openRadioGroup.setOnCheckedChangeListener(this);
        openFromDeviceStorageButton.setOnClickListener(this);

        buttonOpenPrinter.setOnClickListener(this);
        buttonPrint.setOnClickListener(this);
        buttonClosePrinter.setOnClickListener(this);

        seekBar.setOnSeekBarChangeListener(this);

        setBondedDevices();

        arrayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_single_choice, bondedDevices);
        listView.setAdapter(arrayAdapter);

        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listView.setOnItemClickListener(this);

        bxlConfigLoader = new BXLConfigLoader(this);
        try {
            bxlConfigLoader.openFile();
        } catch (Exception e) {
            e.printStackTrace();
            bxlConfigLoader.newFile();
        }

        /**
         * Printer
         */
//        posPrinter = new POSPrinter(this);
//        posPrinter.addErrorListener(this);
//        posPrinter.addStatusUpdateListener(this);
//        posPrinter.addOutputCompleteListener(this);
//        try {
//            posPrinter.setAsyncMode(true);
//        } catch (JposException e) {
//            e.printStackTrace();
//        }

        /**
         * MSR
         */
//		msr = new MSR();

        /**
         * Smart Card
         */
        smartCardRW = new SmartCardRW();
        try {
            smartCardRW.setDataEventEnabled(true);
            smartCardRW.addDataListener(this);
        } catch (JposException e) {
            e.printStackTrace();
        }
    }

    private void buildView() {

        listView = (ListView) findViewById(R.id.listViewPairedDevices);

        pathTextView = (TextView) findViewById(R.id.textViewPath);
        progressTextView = (TextView) findViewById(R.id.textViewProgress);

        openRadioGroup = (RadioGroup) findViewById(R.id.radioGroupOpen);

        openFromDeviceStorageButton = (Button) findViewById(R.id.buttonOpenFromDeviceStorage);
        buttonOpenPrinter = (Button) findViewById(R.id.buttonOpenPrinter);
        buttonPrint = (Button) findViewById(R.id.buttonPrint);
        buttonClosePrinter = (Button) findViewById(R.id.buttonClosePrinter);

        seekBar = (SeekBar) findViewById(R.id.seekBarBrightness);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            /**
             * Printer
             */
//            posPrinter.close();

            /**
             * MSR
             */
//			msr.close();

            /**
             * Smart Card
             */
            smartCardRW.close();
        } catch (JposException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivityForResult(intent, REQUEST_CODE_BLUETOOTH);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_BLUETOOTH:
                setBondedDevices();
                break;

            case REQUEST_CODE_ACTION_PICK:
                if (data != null) {
                    Uri uri = data.getData();
                    ContentResolver cr = getContentResolver();
                    Cursor c = cr.query(uri,
                            new String[]{MediaStore.Images.Media.DATA}, null,
                            null, null);
                    if (c == null || c.getCount() == 0) {
                        return;
                    }

                    c.moveToFirst();
                    int columnIndex = c
                            .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    String text = c.getString(columnIndex);
                    c.close();

                    pathTextView.setText(text);
                }
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonOpenFromDeviceStorage:
                openFromDeviceStorage();
                break;

            case R.id.buttonOpenPrinter:
                openPrinter();
                break;

            case R.id.buttonPrint:
                print();
                break;

            case R.id.buttonClosePrinter:
                closePrinter();
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
        String device = ((TextView) view).getText().toString();

        String name = device.substring(0, device.indexOf(DEVICE_ADDRESS_START));

        String address = device.substring(device.indexOf(DEVICE_ADDRESS_START)
                        + DEVICE_ADDRESS_START.length(),
                device.indexOf(DEVICE_ADDRESS_END));

        try {
            for (Object entry : bxlConfigLoader.getEntries()) {
                JposEntry jposEntry = (JposEntry) entry;
                bxlConfigLoader.removeEntry(jposEntry.getLogicalName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            logicalName = setProductName(name);

            /**
             * Printer
             */
//            bxlConfigLoader.addEntry(logicalName,
//                    BXLConfigLoader.DEVICE_CATEGORY_POS_PRINTER,
//                    logicalName,
//                    BXLConfigLoader.DEVICE_BUS_BLUETOOTH, address);

            /**
             * MSR
             */
//            bxlConfigLoader.addEntry(logicalName,
//                    BXLConfigLoader.DEVICE_CATEGORY_MSR,
//                    logicalName,
//                    BXLConfigLoader.DEVICE_BUS_BLUETOOTH, address);

            /**
             * Smart Card
             */
            bxlConfigLoader.addEntry(logicalName,
                    BXLConfigLoader.DEVICE_CATEGORY_SMART_CARD_RW,
                    logicalName,
                    BXLConfigLoader.DEVICE_BUS_BLUETOOTH, address);

            bxlConfigLoader.saveFile();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private String setProductName(String name) {
        String productName = BXLConfigLoader.PRODUCT_NAME_SPP_R200II;

        if ((name.indexOf("SPP-R200II") >= 0)) {
            if (name.length() > 10) {
                if (name.substring(10, 11).equals("I")) {
                    productName = BXLConfigLoader.PRODUCT_NAME_SPP_R200III;
                }
            }
        } else if ((name.indexOf("SPP-R210") >= 0)) {
            productName = BXLConfigLoader.PRODUCT_NAME_SPP_R210;
        } else if ((name.indexOf("SPP-R310") >= 0)) {
            productName = BXLConfigLoader.PRODUCT_NAME_SPP_R310;
        } else if ((name.indexOf("SPP-R300") >= 0)) {
            productName = BXLConfigLoader.PRODUCT_NAME_SPP_R300;
        } else if ((name.indexOf("SPP-R400") >= 0)) {
            productName = BXLConfigLoader.PRODUCT_NAME_SPP_R400;
        }

        Toast.makeText(this, productName, Toast.LENGTH_SHORT).show();

        return productName;
    }


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress,
                                  boolean fromUser) {
        progressTextView.setText(Integer.toString(progress));
        brightness = progress;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (checkedId) {
            case R.id.radioDeviceStorage:
                openFromDeviceStorageButton.setEnabled(true);
                break;

            case R.id.radioProjectResources:
                openFromDeviceStorageButton.setEnabled(false);
                break;
        }
    }

    private void setBondedDevices() {
        logicalName = null;
        bondedDevices.clear();

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter
                .getDefaultAdapter();
        Set<BluetoothDevice> bondedDeviceSet = bluetoothAdapter
                .getBondedDevices();

        for (BluetoothDevice device : bondedDeviceSet) {
            bondedDevices.add(device.getName() + DEVICE_ADDRESS_START
                    + device.getAddress() + DEVICE_ADDRESS_END);
        }

        if (arrayAdapter != null) {
            arrayAdapter.notifyDataSetChanged();
        }
    }

    private void openFromDeviceStorage() {
        String externalStorageState = Environment.getExternalStorageState();

        if (externalStorageState.equals(Environment.MEDIA_MOUNTED)) {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType(android.provider.MediaStore.Images.Media.CONTENT_TYPE);
            startActivityForResult(intent, REQUEST_CODE_ACTION_PICK);
        }
    }

    private void openPrinter() {

        Log.i(TAG, "logicalName: " + logicalName);

        try {

            /**
             * Printer
             */
//            posPrinter.open(logicalName);
//            posPrinter.claim(0);
//            posPrinter.setDeviceEnabled(true);

            /**
             * MSR
             */
//			msr.open(logicalName);
//			msr.claim(3000);
// 			msr.setDeviceEnabled(true);
//			msr.setDataEventEnabled(true);
//			msr.setAutoDisable(true);
//			msr.addDataListener(this);

            /**
             * Smart Card
             */

            smartCardRW.open(logicalName);
            smartCardRW.claim(0);
            smartCardRW.setDeviceEnabled(true);

//            smartCardRW.setDataEventEnabled(true);

//            smartCardRW.setSCSlot(0x01 << (Integer.SIZE - 1));// Smart Card
//            smartCardRW.setSCSlot(0x01 << (Integer.SIZE - 2)); // SAM1
//            smartCardRW.setSCSlot(0x01 << (Integer.SIZE - 3)); // SAM2
//
//            smartCardRW.setIsoEmvMode(SmartCardRWConst.SC_CMODE_EMV); // EMV Mode
//            smartCardRW.setIsoEmvMode(SmartCardRWConst.SC_CMODE_ISO); // ISO Mode

        } catch (JposException e) {
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();

//            try {
//                /**
//                 * Printer
//                 */
////                posPrinter.close();
//
//                /**
//                 * MSR
//                 */
////				msr.close();
//
//                /**
//                 * Printer
//                 */
////                smartCardRW.close();
//
//            } catch (JposException e1) {
//                e1.printStackTrace();
//            }
        }
    }

    private void closePrinter() {
        try {
            /**
             * Printer
             */
//            posPrinter.close();

            /**
             * MSR
             */
//			msr.close();

            /**
             * Smart Card
             */
            smartCardRW.close();

        } catch (JposException e) {
            e.printStackTrace();
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    private void print() {

        /**
         * Printer
         */
//		InputStream is = null;
//		try {
//			ByteBuffer buffer = ByteBuffer.allocate(4);
//			buffer.put((byte) POSPrinterConst.PTR_S_RECEIPT);
//			buffer.put((byte) brightness);
//			buffer.put((byte) compress);
//			buffer.put((byte) 0x00);
//
//			switch (openRadioGroup.getCheckedRadioButtonId()) {
//			case R.id.radioDeviceStorage:
//
//				posPrinter.printBitmap(buffer.getInt(0), pathTextView.getText().toString(),
//						posPrinter.getRecLineWidth(), POSPrinterConst.PTR_BM_LEFT);
//				break;
//
//			case R.id.radioProjectResources:
//				is = getResources().openRawResource(R.raw.project_resource1);
//				is = getResources().openRawResource(R.raw.bixolon_logo_black_500);
//				Bitmap bitmap = BitmapFactory.decodeStream(is);
//				posPrinter.printBitmap(buffer.getInt(0), bitmap,
//						posPrinter.getRecLineWidth(), POSPrinterConst.PTR_BM_LEFT);
//
//				break;
//			}
//		} catch (JposException e) {
//			e.printStackTrace();
//			Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
//		} finally {
//			if (is != null) {
//				try {
//					is.close();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
//		}

        /**
         * MSR
         */
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    String strData = new String(msr.getTrack1Data());
//                    strData += new String(msr.getTrack2Data());
//                    strData += new String(msr.getTrack3Data());
//                    Toast.makeText(MainActivity.this, strData, Toast.LENGTH_SHORT).show();
//                } catch (JposException e) {
//                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
//                }
//            }
//        });

        /**
         * Smart Card
         */

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {

                    String[] data = new String[]{new String(new byte[]{
                            0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3a, 0x3b, 0x3c, 0x3d, 0x3e, 0x3f, 0x40})
                    };
                    int[] count = new int[1];

                    smartCardRW.readData(SmartCardRWConst.SC_READ_DATA, count, data);
                    int rspSize = count[0];
                    byte[] rspData = data[0].getBytes();

                    Toast.makeText(MainActivity.this, new String(rspData), Toast.LENGTH_SHORT).show();

                } catch (JposException e) {
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void errorOccurred(ErrorEvent errorEvent) {

    }

    @Override
    public void statusUpdateOccurred(StatusUpdateEvent statusUpdateEvent) {

    }

    @Override
    public void outputCompleteOccurred(OutputCompleteEvent outputCompleteEvent) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, "complete print", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void dataOccurred(DataEvent dataEvent) {

        Log.d(TAG, "dataOccurred: " + dataEvent.toString());

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                /**
                 * MSR
                 */
//                try {
//                    String strData = new String(msr.getTrack1Data());
//                    strData += new String(msr.getTrack2Data());
//                    strData += new String(msr.getTrack3Data());
//                    Toast.makeText(MainActivity.this, strData, Toast.LENGTH_SHORT).show();
//                } catch (JposException e) {
//                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
//                }

                /**
                 * Smart Card
                 */
                try {

                    String[] data = new String[]{new String(new byte[]{
                            0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x3a, 0x3b, 0x3c, 0x3d, 0x3e, 0x3f, 0x40})
                    };
                    int[] count = new int[1];

                    smartCardRW.readData(SmartCardRWConst.SC_READ_DATA, count, data);
                    int rspSize = count[0];
                    byte[] rspData = data[0].getBytes();

                    Toast.makeText(MainActivity.this, new String(rspData), Toast.LENGTH_SHORT).show();

                } catch (JposException e) {
                    Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }
            }
        });
    }
}

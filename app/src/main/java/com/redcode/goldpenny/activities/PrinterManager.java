package com.redcode.goldpenny.activities;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import com.redcode.goldpenny.R;
import com.redcode.goldpenny.bluetooth.BluetoothService;
import com.redcode.goldpenny.exceptions.BluetoothConnectionException;
import com.redcode.goldpenny.exceptions.FindPrinterException;
import com.redcode.goldpenny.exceptions.SendDataException;
import com.redcode.goldpenny.model.Product;

import com.redcode.escposxml.EscPosDriver;
import java.io.InputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class PrinterManager {
    private BluetoothService btService = null;
    private Context context;
    /* EscPos driver to parse xml template to esc/pos commands */
    private EscPosDriver escPosDriver;

    /* Data that will be printed on thermal printer */
    private byte[] printData;

    /* XML Template with the print template */
    private InputStream xmlFile;

    private NumberFormat numberFormat = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss");

    public PrinterManager(Activity activity) {
        this.btService = new BluetoothService();

        this.context = activity.getApplicationContext();

        /* We need this to open the xml template from res/raw folder */
        this.xmlFile = activity.getResources().openRawResource(R.raw.print_template);

        /* Creating a esc/pos driver with the given xmlfile */
        this.escPosDriver = new EscPosDriver(this.xmlFile);

    }

    /**
     * Transforms a sale into byte array to print using a xml template.
     * @param products Product list from a sale
     */
    public void print(ArrayList<Product> products) throws FindPrinterException{
        ArrayList<Product> selectedProducts = products;
        this.simpleDateFormat.setTimeZone(TimeZone.getTimeZone("GMT-3"));

        for (Product product : selectedProducts) {
            this.escPosDriver.setTemplateText("product", product.getName());
            this.escPosDriver.setTemplateText("price", this.numberFormat.format(product.getPrice()));
            this.escPosDriver.setTemplateText("date", this.simpleDateFormat.format(new Date()));
            try{
                for (int i = 0; i < product.getQuantity(); i++) {
                    sendProductToPrint(this.escPosDriver.getBytes());
                }
            }catch (FindPrinterException e){
                throw new FindPrinterException();
            }
        }
    }

    /**
     * Send a sale to the bluetooth printer
     * @param productData Sale data
     */
    private void sendProductToPrint(byte[] productData) throws FindPrinterException{
        try {
            this.btService.sendByteData(productData);
        } catch (SendDataException e) {
            e.printStackTrace();
            Toast.makeText(context,
                    "Failed to print",
                    Toast.LENGTH_SHORT).show();
            throw new FindPrinterException();
        }
    }

    public void start() {
        try {
            this.btService.startConnection();
        } catch (BluetoothConnectionException e) {

            Toast.makeText(context,
                    "Bluetooth connection failed. Probably Printer is not available",
                    Toast.LENGTH_LONG).show();
            /* alert dialog saying that
            bluetooth connection failed and return
            to previous activity */
        } catch (RuntimeException e ){
            /* No btService
            * */
        }
    }

    public void destroy() {
        try {
            this.btService.closeConnection();
        } catch (BluetoothConnectionException e) {
            Toast.makeText(context, "FAILED TO CLOSE BLUETOOTH CONNECTION", Toast.LENGTH_SHORT);
        }
    }
}

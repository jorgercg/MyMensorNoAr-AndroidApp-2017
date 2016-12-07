package com.mymensor;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import android.util.Xml;

import org.apache.commons.io.FileUtils;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;

import static java.nio.charset.StandardCharsets.UTF_8;

public class ConfigFileCreator {

    private static final String TAG = "ConfigFileCreator";

    public static void createVpsfile(Context context, File directory, String fileName){
        short assetId = 1;
        String frequencyUnit = Constants.frequencyUnit;
        int frequencyValue = Constants.frequencyValue;
        short qtyVps =2;
        float tolerancePosition = Constants.tolerancePosition;
        float toleranceRotation = Constants.toleranceRotation;
        boolean vpArIsConfigured[] = new boolean[qtyVps];
        vpArIsConfigured[0]=false;
        vpArIsConfigured[1]=false;
        int vpXCameraDistance[] = new int[qtyVps];
        vpXCameraDistance[0]=0;
        vpXCameraDistance[1]=0;
        int vpYCameraDistance[] = new int[qtyVps];
        vpYCameraDistance[0]=0;
        vpYCameraDistance[1]=0;
        int vpZCameraDistance[] = new int[qtyVps];
        vpZCameraDistance[0]=0;
        vpZCameraDistance[1]=0;
        int vpXCameraRotation[] = new int[qtyVps];
        vpXCameraRotation[0]=0;
        vpXCameraRotation[1]=0;
        int vpYCameraRotation[] = new int[qtyVps];
        vpYCameraRotation[0]=0;
        vpYCameraRotation[1]=0;
        int vpZCameraRotation[] = new int[qtyVps];
        vpZCameraRotation[0]=0;
        vpZCameraRotation[1]=0;
        String vpLocationDesText[] = new String[qtyVps+1];
        vpLocationDesText[0] = context.getString(R.string.vp_capture_placeholder_description_freevp);
        vpLocationDesText[1] = context.getString(R.string.vp_capture_placeholder_description)+"1";
        short vpMarkerlessMarkerWidth[] = new short[qtyVps];
        vpMarkerlessMarkerWidth[0] = Constants.standardMarkerlessMarkerWidth;
        vpMarkerlessMarkerWidth[1] = Constants.standardMarkerlessMarkerWidth;
        short vpMarkerlessMarkerHeigth[] = new short[qtyVps];
        vpMarkerlessMarkerHeigth[0] = Constants.standardMarkerlessMarkerHeigth;
        vpMarkerlessMarkerHeigth[1] = Constants.standardMarkerlessMarkerHeigth;
        boolean vpIsAmbiguous[] = new boolean[qtyVps];
        vpIsAmbiguous[0]=false;
        vpIsAmbiguous[1]=false;
        boolean vpFlashTorchIsOn[] = new boolean[qtyVps];
        vpFlashTorchIsOn[0]=false;
        vpFlashTorchIsOn[1]=false;
        boolean vpIsSuperSingle[] = new boolean[qtyVps];
        vpIsSuperSingle[0]=false;
        vpIsSuperSingle[1]=false;
        boolean vpSuperIdIs20mm[] = new boolean[qtyVps];
        vpSuperIdIs20mm[0]=false;
        vpSuperIdIs20mm[1]=false;
        boolean vpSuperIdIs100mm[] = new boolean[qtyVps];
        vpSuperIdIs100mm[0]=false;
        vpSuperIdIs100mm[1]=false;
        int vpSuperMarkerId[] = new int[qtyVps];
        vpSuperMarkerId[0] = 0;
        vpSuperMarkerId[1] = 0;
        // Saving Vps Data initial configuration.
        try
        {
            XmlSerializer xmlSerializer = Xml.newSerializer();
            StringWriter writer = new StringWriter();
            xmlSerializer.setOutput(writer);
            xmlSerializer.startDocument("UTF-8", true);
            xmlSerializer.text("\n");
            xmlSerializer.startTag("","VpsData");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","Parameters");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","AssetId");
            xmlSerializer.text(Short.toString(assetId));
            xmlSerializer.endTag("","AssetId");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","FrequencyUnit");
            xmlSerializer.text(frequencyUnit);
            xmlSerializer.endTag("","FrequencyUnit");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","FrequencyValue");
            xmlSerializer.text(Integer.toString(frequencyValue));
            xmlSerializer.endTag("","FrequencyValue");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","QtyVps");
            xmlSerializer.text(Short.toString(qtyVps));
            xmlSerializer.endTag("","QtyVps");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","TolerancePosition");
            xmlSerializer.text(Float.toString(tolerancePosition));
            xmlSerializer.endTag("","TolerancePosition");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.text("\t");
            xmlSerializer.startTag("","ToleranceRotation");
            xmlSerializer.text(Float.toString(toleranceRotation));
            xmlSerializer.endTag("","ToleranceRotation");
            xmlSerializer.text("\n");
            xmlSerializer.text("\t");
            xmlSerializer.endTag("","Parameters");
            for (int i=0; i<(qtyVps); i++)
            {
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","Vp");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpNumber");
                xmlSerializer.text(Integer.toString(i));
                xmlSerializer.endTag("","VpNumber");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpArIsConfigured");
                xmlSerializer.text(Boolean.toString(vpArIsConfigured[i]));
                xmlSerializer.endTag("","VpArIsConfigured");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpXCameraDistance");
                xmlSerializer.text(Integer.toString(vpXCameraDistance[i]));
                xmlSerializer.endTag("","VpXCameraDistance");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpYCameraDistance");
                xmlSerializer.text(Integer.toString(vpYCameraDistance[i]));
                xmlSerializer.endTag("","VpYCameraDistance");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpZCameraDistance");
                xmlSerializer.text(Integer.toString(vpZCameraDistance[i]));
                xmlSerializer.endTag("","VpZCameraDistance");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpXCameraRotation");
                xmlSerializer.text(Integer.toString(vpXCameraRotation[i]));
                xmlSerializer.endTag("","VpXCameraRotation");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpYCameraRotation");
                xmlSerializer.text(Integer.toString(vpYCameraRotation[i]));
                xmlSerializer.endTag("","VpYCameraRotation");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpZCameraRotation");
                xmlSerializer.text(Integer.toString(vpZCameraRotation[i]));
                xmlSerializer.endTag("","VpZCameraRotation");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpLocDescription");
                xmlSerializer.text(vpLocationDesText[i]);
                xmlSerializer.endTag("","VpLocDescription");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpMarkerlessMarkerWidth");
                xmlSerializer.text(Short.toString(vpMarkerlessMarkerWidth[i]));
                xmlSerializer.endTag("","VpMarkerlessMarkerWidth");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpMarkerlessMarkerHeigth");
                xmlSerializer.text(Short.toString(vpMarkerlessMarkerHeigth[i]));
                xmlSerializer.endTag("","VpMarkerlessMarkerHeigth");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpIsAmbiguous");
                xmlSerializer.text(Boolean.toString(vpIsAmbiguous[i]));
                xmlSerializer.endTag("","VpIsAmbiguous");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpFlashTorchIsOn");
                xmlSerializer.text(Boolean.toString(vpFlashTorchIsOn[i]));
                xmlSerializer.endTag("","VpFlashTorchIsOn");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpIsSuperSingle");
                xmlSerializer.text(Boolean.toString(vpIsSuperSingle[i]));
                xmlSerializer.endTag("","VpIsSuperSingle");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpSuperIdIs20mm");
                xmlSerializer.text(Boolean.toString(vpSuperIdIs20mm[i]));
                xmlSerializer.endTag("","VpSuperIdIs20mm");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","vpSuperIdIs100mm");
                xmlSerializer.text(Boolean.toString(vpSuperIdIs100mm[i]));
                xmlSerializer.endTag("","vpSuperIdIs100mm");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("","VpSuperMarkerId");
                if (vpIsSuperSingle[i])
                {
                    xmlSerializer.text(Integer.toString(vpSuperMarkerId[i]));
                }
                else
                {
                    xmlSerializer.text(Integer.toString(0));
                }
                xmlSerializer.endTag("","VpSuperMarkerId");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("","Vp");
            }
            xmlSerializer.text("\n");
            xmlSerializer.endTag("","VpsData");
            xmlSerializer.endDocument();
            String vpsConfigFileContents = writer.toString();
            try {
                File vpsConfigFile = new File(directory,fileName);
                FileUtils.writeStringToFile(vpsConfigFile,vpsConfigFileContents, UTF_8);
            } catch (Exception e)
            {
                Log.e(TAG, "createVpsfile(): vpsFile creation failed:"+e.toString());
            }
        }
        catch (Exception e)
        {
            Log.e(TAG, "createVpsfile(): vpsFile creation failed:"+e.toString());
        }

    }


    public static void createVpsCheckedFile(Context context, File directory, String fileName){

        short qtyVps=2;
        // Saving vpChecked state.
        try {
            XmlSerializer xmlSerializer = Xml.newSerializer();
            StringWriter writer = new StringWriter();
            xmlSerializer.setOutput(writer);
            xmlSerializer.startDocument("UTF-8", true);
            xmlSerializer.text("\n");
            xmlSerializer.startTag("", "VpsChecked");
            xmlSerializer.text("\n");
            for (int i = 0; i < qtyVps; i++) {
                xmlSerializer.text("\t");
                xmlSerializer.startTag("", "Vp");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("", "VpNumber");
                xmlSerializer.text(Integer.toString(i));
                xmlSerializer.endTag("", "VpNumber");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("", "Checked");
                xmlSerializer.text(Boolean.toString(false));
                xmlSerializer.endTag("", "Checked");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.text("\t");
                xmlSerializer.startTag("", "PhotoTakenTimeMillis");
                xmlSerializer.text(Long.toString(0));
                xmlSerializer.endTag("", "PhotoTakenTimeMillis");
                xmlSerializer.text("\n");
                xmlSerializer.text("\t");
                xmlSerializer.endTag("", "Vp");
                xmlSerializer.text("\n");
            }
            xmlSerializer.endTag("", "VpsChecked");
            xmlSerializer.endDocument();
            String vpsCheckedFileContents = writer.toString();
            try {
                File vpsCheckedFile = new File(directory,fileName);
                FileUtils.writeStringToFile(vpsCheckedFile,vpsCheckedFileContents, UTF_8);
            } catch (Exception e)
            {
                Log.e(TAG, "createVpsCheckedFile(): file creation failed, see stack trace"+e.toString());
            }

        } catch (Exception e) {
            Log.e(TAG, "createVpsCheckedFile saving to Remote Storage failed:"+e.toString());
        }
    }


    public static void createDescvpFile(Context context, File directory, String fileName){

        String internalAssetsFileName = "mymensordescvp.png";
        AssetManager assetManager = context.getAssets();
        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(internalAssetsFileName);
            File outFile = new File(directory, fileName);
            out = new FileOutputStream(outFile);
            copyFile(in, out);
        } catch(IOException e) {
            Log.e(TAG, "createDescvpFile: Failed to copy asset file: " + fileName, e);
        }
        finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // NOOP
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // NOOP
                }
            }
        }
    }


    public static void createMarkervpFile(Context context, File directory, String fileName){

        String internalAssetsFileName = "mymensormarkervpbw.jpg";
        AssetManager assetManager = context.getAssets();
        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(internalAssetsFileName);
            File outFile = new File(directory, fileName);
            out = new FileOutputStream(outFile);
            copyFile(in, out);
        } catch(IOException e) {
            Log.e(TAG, "createMarkervpFile: Failed to copy asset file: " + fileName, e);
        }
        finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // NOOP
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // NOOP
                }
            }
        }
    }



    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

}

package net.kdt.pojavlaunch.modloaders.modpacks;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.fragment.app.FragmentActivity;

import com.movtery.zalithlauncher.feature.download.item.ModLoaderWrapper;
import com.movtery.zalithlauncher.feature.download.utils.ModLoaderUtils;
import com.movtery.zalithlauncher.ui.dialog.SelectRuntimeDialog;

import net.kdt.pojavlaunch.JavaGUILauncherActivity;
import net.kdt.pojavlaunch.Tools;

import java.io.File;

/**
 * This class is meant to track the availability of a modloader that is ready to be installed (as a result of modpack installation)
 * It is needed because having all  this logic spread over LauncherActivity would be clumsy, and I think that this is the best way to
 * ensure that the modloader installer will run, even if the user does not receive the notification or something else happens
 */
public class ModloaderInstallTracker implements SharedPreferences.OnSharedPreferenceChangeListener {
    private final SharedPreferences mSharedPreferences;
    private final FragmentActivity mActivity;

    /**
     * Create a ModInstallTracker object. This must be done in the Activity's onCreate method.
     * @param activity the host activity
     */
    public ModloaderInstallTracker(FragmentActivity activity) {
        mActivity = activity;
        mSharedPreferences = getPreferences(activity);

    }

    /**
     * Attach the ModloaderInstallTracker to the current Activity. Must be done in the Activity's
     * onResume method
     */
    public void attach() {
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
        runCheck();
    }

    /**
     * Detach the ModloaderInstallTracker from the current Activity. Must be done in the Activity's
     * onPause method
     */
    public void detach() {
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String prefName) {
        if(!"modLoaderAvailable".equals(prefName)) return;
        runCheck();
    }

    @SuppressLint("ApplySharedPref")
    private void runCheck() {
        if(!mSharedPreferences.getBoolean("modLoaderAvailable", false)) return;
        SharedPreferences.Editor editor = mSharedPreferences.edit().putBoolean("modLoaderAvailable", false);
        if(!editor.commit()) editor.apply();
        ModLoaderWrapper modLoader = deserializeModLoader(mSharedPreferences);
        File modInstallFile = deserializeInstallFile(mSharedPreferences);
        if(modLoader == null || modInstallFile == null) return;
        startModInstallation(modLoader, modInstallFile);
    }

    private void startModInstallation(ModLoaderWrapper modLoader, File modInstallFile) {
        Intent installIntent = modLoader.getInstallationIntent(mActivity, modInstallFile);
        SelectRuntimeDialog selectRuntimeDialog = new SelectRuntimeDialog(mActivity);
        selectRuntimeDialog.setListener(jreName -> {
            installIntent.putExtra(JavaGUILauncherActivity.EXTRAS_JRE_NAME, jreName);
            selectRuntimeDialog.dismiss();
            Tools.backToMainMenu(mActivity);
            mActivity.startActivity(installIntent);
        });
        selectRuntimeDialog.show();
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences("modloader_info", Context.MODE_PRIVATE);
    }

    /**
     * Store the data necessary to start a ModLoader installation for the tracker to start the installer
     * sometime.
     * @param context the Context
     * @param modLoader the ModLoader to store
     * @param modInstallFile the installer jar to store
     */
    @SuppressLint("ApplySharedPref")
    public static void saveModLoader(Context context, ModLoaderWrapper modLoader, File modInstallFile) {
        SharedPreferences.Editor editor = getPreferences(context).edit();
        editor.putInt("modLoaderType", modLoader.getModLoader().getType());
        editor.putString("modLoaderVersion", modLoader.getModLoaderVersion());
        editor.putString("minecraftVersion", modLoader.getMinecraftVersion());
        editor.putString("modInstallerJar", modInstallFile.getAbsolutePath());
        editor.putBoolean("modLoaderAvailable", true);
        editor.commit();
    }

    private static ModLoaderWrapper deserializeModLoader(SharedPreferences sharedPreferences) {
        if(!sharedPreferences.contains("modLoaderType") ||
        !sharedPreferences.contains("modLoaderVersion") ||
        !sharedPreferences.contains("minecraftVersion")) return null;
        return new ModLoaderWrapper(ModLoaderUtils.Companion.getModLoader(sharedPreferences.getInt("modLoaderType", -1)),
                sharedPreferences.getString("modLoaderVersion", ""),
                sharedPreferences.getString("minecraftVersion", ""));
    }

    private static File deserializeInstallFile(SharedPreferences sharedPreferences) {
        if(!sharedPreferences.contains("modInstallerJar")) return null;
        return new File(sharedPreferences.getString("modInstallerJar", ""));
    }
}

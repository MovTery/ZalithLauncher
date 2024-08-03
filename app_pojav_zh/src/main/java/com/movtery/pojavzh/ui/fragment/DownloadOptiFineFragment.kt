package com.movtery.pojavzh.ui.fragment

import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.movtery.pojavzh.feature.mod.modloader.BaseModVersionListAdapter
import com.movtery.pojavzh.feature.mod.modloader.OptiFineDownloadType
import com.movtery.pojavzh.ui.dialog.SelectRuntimeDialog
import com.movtery.pojavzh.ui.subassembly.twolevellist.ModListAdapter
import com.movtery.pojavzh.ui.subassembly.twolevellist.ModListFragment
import com.movtery.pojavzh.ui.subassembly.twolevellist.ModListItemBean
import com.movtery.pojavzh.utils.MCVersionComparator.versionCompare
import net.kdt.pojavlaunch.JavaGUILauncherActivity
import net.kdt.pojavlaunch.PojavApplication
import net.kdt.pojavlaunch.R
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.modloaders.ModloaderDownloadListener
import net.kdt.pojavlaunch.modloaders.ModloaderListenerProxy
import net.kdt.pojavlaunch.modloaders.OptiFineDownloadTask
import net.kdt.pojavlaunch.modloaders.OptiFineUtils
import net.kdt.pojavlaunch.modloaders.OptiFineUtils.OptiFineVersion
import net.kdt.pojavlaunch.modloaders.OptiFineUtils.OptiFineVersions
import java.io.File
import java.util.concurrent.Future
import java.util.function.Consumer

class DownloadOptiFineFragment : ModListFragment(), ModloaderDownloadListener {
    companion object {
        const val TAG: String = "DownloadOptiFineFragment"
        const val BUNDLE_DOWNLOAD_MOD: String = "bundle_download_mod"
    }

    private val modloaderListenerProxy = ModloaderListenerProxy()
    private var mIsDownloadMod = false

    override fun init() {
        setIcon(ContextCompat.getDrawable(activity, R.drawable.ic_optifine))
        setNameText("OptiFine")
        setReleaseCheckBoxGone()
        parseBundle()
        super.init()
    }

    override fun refresh(): Future<*> {
        return PojavApplication.sExecutorService.submit {
            try {
                Tools.runOnUiThread {
                    cancelFailedToLoad()
                    componentProcessing(true)
                }
                val optiFineVersions = OptiFineUtils.downloadOptiFineVersions()
                processModDetails(optiFineVersions)
            } catch (e: Exception) {
                Tools.runOnUiThread {
                    componentProcessing(false)
                    setFailedToLoad(e.toString())
                }
            }
        }
    }

    private fun parseBundle() {
        val bundle = arguments ?: return
        mIsDownloadMod = bundle.getBoolean(BUNDLE_DOWNLOAD_MOD, false)
    }

    private fun processModDetails(optiFineVersions: OptiFineVersions?) {
        if (optiFineVersions == null) {
            Tools.runOnUiThread {
                componentProcessing(false)
                setFailedToLoad("optiFineVersions is Empty!")
            }
            return
        }
        val currentTask = currentTask

        val mOptiFineVersions: MutableMap<String, MutableList<OptiFineVersion?>> = HashMap()
        optiFineVersions.optifineVersions.forEach(Consumer<List<OptiFineVersion>> { optiFineVersionList: List<OptiFineVersion> ->  //通过版本列表一层层遍历并合成为 Minecraft版本 + Optifine版本的Map集合
            if (currentTask.isCancelled) return@Consumer

            optiFineVersionList.forEach(Consumer Consumer2@{ optiFineVersion: OptiFineVersion ->
                if (currentTask.isCancelled) return@Consumer2
                mOptiFineVersions.computeIfAbsent(optiFineVersion.minecraftVersion) { ArrayList() }
                    .add(optiFineVersion)
            })
        })

        if (currentTask.isCancelled) return

        val mData: MutableList<ModListItemBean> = ArrayList()
        mOptiFineVersions.entries
            .sortedWith { entry1, entry2 ->
                versionCompare(entry1.key, entry2.key)
            }
            .forEach { entry: Map.Entry<String, List<OptiFineVersion?>> ->
                if (currentTask.isCancelled) return@forEach

                val adapter = BaseModVersionListAdapter(modloaderListenerProxy, this, R.drawable.ic_optifine, entry.value)

                adapter.setOnItemClickListener { version: Any? ->
                    Thread(OptiFineDownloadTask(version as OptiFineVersion?, modloaderListenerProxy,
                            if (mIsDownloadMod) OptiFineDownloadType.DOWNLOAD_MOD else OptiFineDownloadType.DOWNLOAD_GAME)
                    ).start()
                }
                mData.add(ModListItemBean(entry.key, adapter))
            }

        if (currentTask.isCancelled) return

        Tools.runOnUiThread {
            val recyclerView = recyclerView
            try {
                var mModAdapter = recyclerView.adapter as ModListAdapter?
                if (mModAdapter == null) {
                    mModAdapter = ModListAdapter(this, mData)
                    recyclerView.layoutManager = LinearLayoutManager(activity)
                    recyclerView.adapter = mModAdapter
                } else {
                    mModAdapter.updateData(mData)
                }
            } catch (ignored: Exception) {
            }

            componentProcessing(false)
            recyclerView.scheduleLayoutAnimation()
        }
    }

    override fun onDownloadFinished(downloadedFile: File) {
        if (!mIsDownloadMod) {
            Tools.runOnUiThread {
                val modInstallerStartIntent = Intent(activity, JavaGUILauncherActivity::class.java)
                OptiFineUtils.addAutoInstallArgs(modInstallerStartIntent, downloadedFile)
                val selectRuntimeDialog = SelectRuntimeDialog(activity)
                selectRuntimeDialog.setListener { jreName: String? ->
                    modloaderListenerProxy.detachListener()
                    modInstallerStartIntent.putExtra(JavaGUILauncherActivity.EXTRAS_JRE_NAME, jreName)
                    selectRuntimeDialog.dismiss()
                    Tools.backToMainMenu(activity)
                    activity.startActivity(modInstallerStartIntent)
                }
                selectRuntimeDialog.show()
            }
        }
    }

    override fun onDataNotAvailable() {
        Tools.runOnUiThread {
            modloaderListenerProxy.detachListener()
            Tools.dialog(activity, activity.getString(R.string.global_error), activity.getString(R.string.of_dl_failed_to_scrape))
        }
    }

    override fun onDownloadError(e: Exception) {
        Tools.runOnUiThread {
            modloaderListenerProxy.detachListener()
            Tools.showError(activity, e)
        }
    }
}

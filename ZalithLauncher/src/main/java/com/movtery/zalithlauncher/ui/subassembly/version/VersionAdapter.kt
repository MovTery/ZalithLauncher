package com.movtery.zalithlauncher.ui.subassembly.version

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayout
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.databinding.ItemVersionBinding
import com.movtery.zalithlauncher.feature.version.Version
import com.movtery.zalithlauncher.feature.version.VersionIconUtils
import net.kdt.pojavlaunch.Tools

class VersionAdapter(private val listener: OnVersionItemClickListener) : RecyclerView.Adapter<VersionAdapter.ViewHolder>() {
    private val versions: MutableList<Version?> = ArrayList()

    @SuppressLint("NotifyDataSetChanged")
    fun refreshVersions(versions: List<Version?>) {
        this.versions.clear()
        this.versions.addAll(versions)

        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemVersionBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(versions[position])
    }

    override fun getItemCount(): Int = versions.size

    inner class ViewHolder(val binding: ItemVersionBinding) : RecyclerView.ViewHolder(binding.root) {
        private val mContext = binding.root.context

        fun bind(version: Version?) {
            binding.versionInfo.removeAllViews()

            version?.let {
                binding.version.text = it.getVersionName()

                if (!it.isValid()) {
                    binding.versionInfo.addView(getInfoTextView(mContext.getString(R.string.version_manager_invalid), true))
                }

                it.getVersionInfo()?.let { versionInfo ->
                    binding.versionInfo.addView(getInfoTextView(versionInfo.minecraftVersion))
                    versionInfo.loaderInfo.forEach { loaderInfo ->
                        binding.versionInfo.addView(getInfoTextView("${loaderInfo.name} ${loaderInfo.version}"))
                    }
                }

                VersionIconUtils(it).start(binding.versionIcon)

                binding.root.setOnClickListener { _ ->
                    listener.onVersionClick(it)
                }
                return
            }
            binding.versionIcon.setImageDrawable(ContextCompat.getDrawable(binding.root.context, R.drawable.ic_add))
            binding.version.setText(R.string.version_install_new)
            binding.root.setOnClickListener { listener.onCreateVersion() }
        }

        private fun getInfoTextView(string: String, setRed: Boolean = false): TextView {
            val textView = TextView(mContext)
            textView.text = string
            val layoutParams = FlexboxLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            layoutParams.setMargins(0, 0, Tools.dpToPx(8f).toInt(), 0)
            textView.layoutParams = layoutParams
            if (setRed) textView.setTextColor(Color.RED)
            return textView
        }


    }

    interface OnVersionItemClickListener {
        fun onVersionClick(version: Version)
        fun onCreateVersion()
    }
}
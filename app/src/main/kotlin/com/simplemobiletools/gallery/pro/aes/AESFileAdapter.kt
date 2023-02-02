package com.simplemobiletools.gallery.pro.aes

import com.simplemobiletools.commons.adapters.MyRecyclerViewAdapter

import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.ColorFilter
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.color.MaterialColors
import com.qtalk.recyclerviewfastscroller.RecyclerViewFastScroller
import com.simplemobiletools.commons.activities.BaseSimpleActivity
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.getFilePlaceholderDrawables
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.commons.views.MyRecyclerView
import com.simplemobiletools.gallery.pro.R
import kotlinx.android.synthetic.main.aes_file_item.view.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.time.toDuration

class AESFileAdapter(
    val activityN: AESActivity, val fileDirItems: List<AESDirItem>, recyclerView: MyRecyclerView,
    itemClick: (Any) -> Unit
) : MyRecyclerViewAdapter(activityN, recyclerView, itemClick), RecyclerViewFastScroller.OnPopupTextUpdate {

    private lateinit var fileDrawable: Drawable
    private lateinit var folderDrawable: Drawable
    private var fileDrawables = HashMap<String, Drawable>()
    private lateinit var colorTintList: ColorStateList
    private val hasOTGConnected = activity.hasOTGConnected()
    private val cornerRadius = resources.getDimension(R.dimen.rounded_corner_radius_small).toInt()
    private val dateFormat = activity.baseConfig.dateFormat
    private val timeFormat = activity.getTimeFormat()
    private var selectableItemCount = 0

    init {
        initDrawables()
        selectableItemCount = fileDirItems.count { !it.isDirectory }
    }

    override fun getActionMenuId() = R.menu.menu_aes_select

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = createViewHolder(R.layout.aes_file_item, parent)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val fileDirItem = fileDirItems[position]
        holder.bindView(fileDirItem, true, true) { itemView, adapterPosition ->
            setupView(itemView, fileDirItem, adapterPosition)
        }
        bindViewHolder(holder)
        if (fileDirItem.isDirectory) {
            holder.itemView.setOnLongClickListener(null)
        }
    }

    override fun getItemCount() = fileDirItems.size

    override fun prepareActionMode(menu: Menu) {

    }

    override fun actionItemPressed(id: Int) {
        activityN.onActionItemClick(id)
    }

    override fun getSelectableItemCount() = selectableItemCount

    override fun getIsItemSelectable(position: Int) = !fileDirItems[position].isDirectory

    override fun getItemKeyPosition(key: Int) = fileDirItems.indexOfFirst { it.path.hashCode() == key }

    override fun getItemSelectionKey(position: Int) = fileDirItems[position].path.hashCode()

    override fun onActionModeCreated() {}

    override fun onActionModeDestroyed() {}

    fun getSelectedItems(): ArrayList<AESDirItem> {
        return selectedKeys.mapNotNull { fileDirItems.getOrNull(getItemKeyPosition(it)) } as ArrayList<AESDirItem>
    }


    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        if (!activity.isDestroyed && !activity.isFinishing) {
            Glide.with(activity).clear(holder.itemView.list_item_icon!!)
        }
    }

    private fun setupView(view: View, fileDirItem: AESDirItem, position: Int) {
        val isItemSelected = selectedKeys.contains(getItemSelectionKey(position))
        view.apply {
            isActivated = isItemSelected
            list_item_display_name.text = fileDirItem.displayName.ifEmpty { fileDirItem.name }
            list_item_display_name.setTextColor(textColor)

            list_item_name.text = fileDirItem.name
            list_item_name.setTextColor(textColor)

            list_item_details.setTextColor(textColor)

            if (fileDirItem.fileInfo != null && fileDirItem.fileInfo!!.duration > 0) {
                list_item_duration.beVisible()
                list_item_duration.setText("${(fileDirItem.fileInfo!!.duration / 1000).toInt().getFormattedDuration()}")
            } else {
                list_item_duration.beGone()
            }

            if (fileDirItem.isDirectory) {
                list_item_icon.setImageDrawable(folderDrawable)
                list_item_details.text = getChildrenCnt(fileDirItem)

            } else {
                list_item_details.text = "${fileDirItem.size.formatSize()} | ${fileDirItem.fileInfo?.lastMod?.formatDate(activity)}"
                val path = fileDirItem.path
                val placeholder = fileDrawables.getOrElse(fileDirItem.name.substringAfterLast(".").toLowerCase(Locale.getDefault()), { fileDrawable })
                val options = RequestOptions()
                    .signature(fileDirItem.getKey())
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .centerCrop()
                    .error(placeholder)

                var itemToLoad = if (fileDirItem.name.endsWith(".apk", true)) {
                    val packageInfo = context.packageManager.getPackageArchiveInfo(path, PackageManager.GET_ACTIVITIES)
                    if (packageInfo != null) {
                        val appInfo = packageInfo.applicationInfo
                        appInfo.sourceDir = path
                        appInfo.publicSourceDir = path
                        appInfo.loadIcon(context.packageManager)
                    } else {
                        path
                    }
                } else {
                    path
                }

                if (!activity.isDestroyed && !activity.isFinishing) {
                    if (fileDirItem.mThumbFile != null) {
                        itemToLoad = AESImageModel(fileDirItem.mThumbFile!!.absolutePath)
                    } else if (activity.isRestrictedSAFOnlyRoot(path)) {
                        itemToLoad = activity.getAndroidSAFUri(path)
                    } else if (hasOTGConnected && itemToLoad is String && activity.isPathOnOTG(itemToLoad)) {
                        itemToLoad = itemToLoad.getOTGPublicPath(activity)
                    }

                    if (itemToLoad.toString().isGif()) {
                        Glide.with(activity).asBitmap().load(itemToLoad).apply(options).into(list_item_icon)
                    } else {
                        Glide.with(activity)
                            .load(itemToLoad)
                            .transition(withCrossFade())
                            .apply(options)
                            .transform(CenterCrop(), RoundedCorners(cornerRadius))
                            .into(list_item_icon)
                    }
                }
            }
            list_item_icon.imageTintList = if (isItemSelected) colorTintList else null
            list_item_selected.beVisibleIf(isItemSelected)
        }
    }

    private fun getChildrenCnt(item: FileDirItem): String {
        val children = item.children
        return activity.resources.getQuantityString(R.plurals.items, children, children)
    }

    private fun initDrawables() {
        folderDrawable = resources.getColoredDrawableWithColor(
            R.drawable.ic_folder_vector,
            MaterialColors.getColor(activity, android.R.attr.colorPrimary, activity.getColor(R.color.md_grey_400))
        )
        folderDrawable.alpha = 180
        fileDrawable = resources.getDrawable(R.drawable.ic_file_generic)
        fileDrawables = getFilePlaceholderDrawables(activity)
        colorTintList = ColorStateList.valueOf(ContextCompat.getColor(activity, R.color.selected_tint_color))
    }

    override fun onChange(position: Int) = fileDirItems.getOrNull(position)?.getBubbleText(activity, dateFormat, timeFormat) ?: ""
}

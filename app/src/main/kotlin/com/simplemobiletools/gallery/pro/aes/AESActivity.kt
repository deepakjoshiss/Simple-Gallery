package com.simplemobiletools.gallery.pro.aes

import android.app.Activity
import android.content.*
import android.os.Bundle
import android.os.Environment
import android.os.Parcelable
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.*
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import com.simplemobiletools.commons.dialogs.ConfirmationDialog
import com.simplemobiletools.commons.dialogs.FilePickerDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.IS_FROM_GALLERY
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import com.simplemobiletools.commons.models.FileDirItem
import com.simplemobiletools.commons.views.Breadcrumbs
import com.simplemobiletools.gallery.pro.BuildConfig
import com.simplemobiletools.gallery.pro.R
import com.simplemobiletools.gallery.pro.activities.MainActivity
import com.simplemobiletools.gallery.pro.activities.SimpleActivity
import com.simplemobiletools.gallery.pro.activities.ViewPagerActivity
import com.simplemobiletools.gallery.pro.extensions.launchAbout
import com.simplemobiletools.gallery.pro.helpers.*
import kotlinx.android.synthetic.main.activity_aes.*
import kotlinx.coroutines.MainScope
import java.io.File


private const val DEFAULT_PIN = "1111"
private const val DEFAULT_PIN_LENGTH = 4

class AESActivity : SimpleActivity(), OnClickListener {
    private var mStartForResult: ActivityResultLauncher<Intent>? = null
    private var currPath: String = Environment.getExternalStorageDirectory().toString()
    private var vaultPath: String? = null

    private var mFirstUpdate = true
    private var mPrevPath = ""
    private var mScrollStates = HashMap<String, Parcelable>()
    private var adapter: AESFileAdapter? = null
    private var mPathsToMove: ArrayList<*>? = null
    private var mActionType: AESTaskType? = null
    private var mBreadcrumbs: AESBreadcrumbs? = null

    private lateinit var mReceiver: BroadcastReceiver

    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_aes)
        mPathsToMove = intent.getStringArrayListExtra("paths")?.also { mActionType = AESTaskType.ENCRYPT }

        if (BuildConfig.DEBUG) {
            val dec = AESUtils.decryptVault(Gson().fromJson(aesConfig.aesVault, AESVault::class.java), "4399")
            if (dec == null) {
                println(">>>> wrong pass}")
                launchAbout()
                return
            }
            onVaultFound(dec[1].decodeToString(), dec[0])
        }

        if (aesConfig.aesVault.isNotEmpty()) {
            val dec = AESUtils.decryptVault(Gson().fromJson(aesConfig.aesVault, AESVault::class.java), "4399")
            println(">>>> ${dec?.get(0)?.decodeToString()}, ${dec?.get(1)?.decodeToString()}")
        }
        setUpPinView()

        mReceiver = object : BroadcastReceiver() {
            var lastCompleted = 0
            override fun onReceive(contxt: Context?, intent: Intent?) {
                when (intent?.action) {
                    AES_TASK_UPDATE -> {
                        val count = intent.getIntExtra(AES_TASK_COMPLETE_COUNT, 0)
                        val shouldUpdate = count != lastCompleted && intent.getBooleanExtra("hasEncrypt", false)
                        //linePrint("action Brodcast rec $count $lastCompleted $shouldUpdate")
                        if (shouldUpdate) {
                            lastCompleted = count
                            tryUpdateItems()
                        }
                    }
                }
            }
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, IntentFilter(AES_TASK_UPDATE))

        mStartForResult = registerForActivityResult<Intent, ActivityResult>(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                if (result.data?.clipData != null) {
                    val n: Int = result.data!!.clipData!!.itemCount
                    for (i in 0 until n) {
                        val item: ClipData.Item = result.data!!.clipData!!.getItemAt(i)
                        startEncryption(item.uri.toString())
                    }
                } else {
                    val path = result.data?.extras?.getString("path")
                    if (path != null) {
                        println(">>>> File path is $path ,, ${result.data?.data}")
                        startEncryption(path)
                    }
                }

            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        AESHelper.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mReceiver)
    }

    override fun onBackPressed() {
        if (vaultPath != null) {
            if (currPath != vaultPath) {
                setCurrentPath(currPath.getParentPath())
                return
            }
        }

        super.onBackPressed()
    }

    private fun onVaultFound(folderPath: String, token: ByteArray) {
        println(">>>> vault is $folderPath ${token.decodeToString()} ${DATA_IV.encodeToByteArray().size}")
        AESHelper.setToken(token)
        vaultPath = folderPath
        filepicker_holder.visibility = View.VISIBLE
        container.removeAllViews()
        updateToolbar()
        setUpFolderView()
    }

    private fun addToVault(fileType: AESFileTypes): Boolean {

        if (fileType == AESFileTypes.Album) {
            openAddFolderDialog()
            return true
        }

        val intent = Intent(this@AESActivity, MainActivity::class.java).apply {
            action = Intent.ACTION_PICK
            type = if (fileType == AESFileTypes.Image) MediaStore.Images.Media.CONTENT_TYPE else MediaStore.Video.Media.CONTENT_TYPE
            putExtra(GET_VIDEO_INTENT, fileType == AESFileTypes.Video)
            putExtra(GET_IMAGE_INTENT, fileType == AESFileTypes.Image)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        mStartForResult?.launch(intent)

        return true
    }

    private fun createAlbum(name: String): Boolean {
        if (AESFileUtils.createAlbum(AESHelper.encryptionCypher, currPath, name)) {
            tryUpdateItems()
            return true
        } else {
            linePrint("Could not create folder $name at $currPath")
        }
        return false
    }

    private fun openAddFolderDialog() {
        val callback = object : TextSubmitCallback {
            override fun onSubmit(text: String, meta: String?) {
                linePrint("submit $text")
                createAlbum(text)
            }

            override fun onTextChange(text: String, meta: String?) {

            }

        }
        AESDonateDialog(this, "Album Name", callback)
    }

    private fun resetVault(): Boolean {
        ConfirmationDialog(this, "Reset Vault?") {
            filepicker_holder.visibility = View.GONE
            aesConfig.aesVault = ""
            vaultPath = null
            setUpPinView()
        }
        return true
    }

    private fun updateToolbar() {
        toolbar.removeView(mBreadcrumbs)
        toolbar.inflateMenu(R.menu.menu_aes)
        toolbar.setTitleTextAppearance(this, R.style.styleBreadcrumbs)
        mBreadcrumbs = LayoutInflater.from(this).inflate(R.layout.aes_breadcrumbs, toolbar, false) as AESBreadcrumbs
        mBreadcrumbs!!.setBasePath(vaultPath ?: "")
        mBreadcrumbs!!.listener = object : AESBreadcrumbs.BreadcrumbsListener {
            override fun breadcrumbClicked(id: Int) {
                linePrint(mBreadcrumbs?.getItem(id)?.path ?: vaultPath ?: "")
                setCurrentPath(mBreadcrumbs?.getItem(id)?.path ?: vaultPath ?: "")
            }

        }
        toolbar.addView(
            mBreadcrumbs,
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
        toolbar.setNavigationOnClickListener {
            vaultPath?.let {
                if (it != currPath) {
                    setCurrentPath(it)
                    return@setNavigationOnClickListener
                }
            }
            finish()
        }
        toolbar.setOnMenuItemClickListener {
            onActionItemClick(it.itemId)
        }
    }

    private fun updateToolbarText() {
        mBreadcrumbs?.setBreadcrumb(currPath)
    }

    private fun setCurrentPath(path: String) {
        if (path == currPath) return
        currPath = path
        tryUpdateItems()
        updateToolbarText()
    }

    private fun decryptSelectedFiles(): Boolean {
        adapter?.getSelectedItems()?.let {
            linePrint("Selected items ${it.size}")
            AESHelper.tasker.showView(this)
            AESHelper.startDecryption(it, File(Environment.getExternalStorageDirectory(), "Backdrops").absolutePath)
        }
        return true
    }

    private fun moveSelectedFiles(): Boolean {
        adapter?.getSelectedItems()?.let {
            adapter!!.finishActMode()
            resetActionData()
            mActionType = AESTaskType.MOVE
            mPathsToMove = it
            setUpFABView()
        }
        return true
    }

    private fun deleteSelectedFiles(): Boolean {
        adapter?.getSelectedItems()?.let {
            adapter!!.finishActMode()
            resetActionData()
            ConfirmationDialog(this, "Delete Selected Files") {

            }
        }
        return true
    }

    private fun resetActionData() {
        mPathsToMove = null
        mActionType = null
        setUpFABView()
    }

    @Suppress("UNCHECKED_CAST")
    private fun performAction() {
        if (mPathsToMove.isNullOrEmpty() || mActionType == null) {
            resetActionData()
            return
        }
        val actionType = mActionType
        val paths: ArrayList<*> = mPathsToMove as ArrayList<*>
        resetActionData()
        linePrint("performing action ${paths.size} ${actionType}")
        when (actionType) {
            AESTaskType.ENCRYPT -> {
                paths.forEach {
                    if (it is String) {
                        startEncryption(it)
                    }
                }
            }
            AESTaskType.DECRYPT -> {

            }

            AESTaskType.MOVE -> {
                moveFiles(paths as ArrayList<AESDirItem>, paths[0].path.getParentPath(), currPath) {
                    tryUpdateItems()
                }
            }
            else -> {}
        }
    }

    private fun cancelAction() {
        ConfirmationDialog(this, "Cancel Move") {
            resetActionData()
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            fab_action.id -> performAction()
            cancel_action.id -> cancelAction()
        }
    }

    fun onActionItemClick(itemId: Int): Boolean {
        return when (itemId) {
            R.id.add_album -> addToVault(AESFileTypes.Album)
            R.id.add_image -> addToVault(AESFileTypes.Image)
            R.id.add_video -> addToVault(AESFileTypes.Video)
            R.id.reset -> resetVault()
            R.id.delete -> true
            R.id.decrypt -> decryptSelectedFiles()
            R.id.move -> moveSelectedFiles()
            else -> false
        }
    }

    private fun createPinCallback(view: ClassicLockView): TextSubmitCallback {
        return object : TextSubmitCallback {
            var startPin = true
            var pinValue: String? = null
            override fun onSubmit(text: String, meta: String?) {
                val vaultData = if (aesConfig.aesVault.isEmpty()) null else Gson().fromJson(aesConfig.aesVault, AESVault::class.java)

                if (vaultData == null) {
                    if (startPin) {
                        if (text == DEFAULT_PIN) {
                            view.resetInput()
                            view.setLabel("Please Enter Pin")
                            startPin = false;
                        } else {
                            launchAbout()
                        }
                        return;
                    }

                    if (text.isEmpty() || text.length != DEFAULT_PIN_LENGTH) {
                        toast("Pin should be $DEFAULT_PIN_LENGTH digits long")
                        return
                    }

                    if (pinValue.isNullOrEmpty()) {
                        pinValue = text
                        view.resetInput()
                        view.setLabel("Please Enter Pin Again")
                        return
                    }

                    if (pinValue == text) {
                        println(">>>>>> Pin matched $pinValue")
                        setUpPassView(text)
                        return
                    }
                    toast("Pin did not match")
                    return
                }

                startPin = false;
                val dec = AESUtils.decryptVault(vaultData, text)
                if (dec == null) {
                    println(">>>> wrong pass}")
                    launchAbout()
                    return
                }
                onVaultFound(dec[1].decodeToString(), dec[0])
            }

            override fun onTextChange(text: String, meta: String?) {
                // ignore
            }

        }
    }

    private fun setUpPinView() {
        if (!vaultPath.isNullOrEmpty()) return
        container.removeAllViews();
        val view: ClassicLockView = layoutInflater.inflate(R.layout.password_classic_bottom, container, false) as ClassicLockView;
        view.setPassCallback(createPinCallback(view))
        container.addView(view)
    }

    private fun setUpPassView(pin: String) {
        container.removeAllViews();
        val view = layoutInflater.inflate(R.layout.password_layout, container, true)
        val passView = view.findViewById<TextInputLayout>(R.id.pass)
        val conPassView = view.findViewById<TextInputLayout>(R.id.confirm_pass)
        val folderPathText = view.findViewById<TextInputEditText>(R.id.folder_path)
        var folderPath: String? = null
        folderPathText.setOnClickListener { _ ->
            FilePickerDialog(this@AESActivity, currPath, false, true) { path ->
                println(">>>>> Picked Folder $path")
                folderPathText.setText(path)
                folderPath = path
            }
        }

        view.findViewById<View>(R.id.confirm_button).setOnClickListener(object : OnClickListener {
            override fun onClick(v: View?) {
                if (folderPath.isNullOrEmpty()) {
                    toast("Please select folder", Toast.LENGTH_SHORT)
                    return;
                }
                if (conPassView.editText?.text.toString() != passView.editText?.text.toString()) {
                    toast("Passwords do not match", Toast.LENGTH_SHORT)
                    return;
                }
                val vault = AESUtils.encryptVault(passView.editText?.text.toString(), folderPath!!, pin)
                if (vault != null) {
                    println(">>>>> ${vault.pass} ${vault.vault}")
                    val dec = AESUtils.decryptVault(vault, pin)
                    println(">>>> ${dec?.get(0)?.decodeToString()}, ${dec?.get(1)?.decodeToString()}")
                }
                aesConfig.aesVault = Gson().toJson(vault)
                setUpPinView()
            }

        })
    }

    private fun setUpFABView() {
        if (!vaultPath.isNullOrEmpty() && !mPathsToMove.isNullOrEmpty() && mActionType != null) {
            fab_action.beVisible()
            fab_text.setText(if (mActionType == AESTaskType.ENCRYPT) "Encrypt here" else "Move here")
            fab_action.setOnClickListener(this)
            cancel_action.setOnClickListener(this)
        } else {
            fab_action.beGone()
        }
    }

    private fun setUpFolderView() {
        setUpFABView()
        vaultPath?.let { currPath = vaultPath!! }
        if (!getDoesFilePathExist(currPath)) {
            currPath = internalStoragePath
        }

        if (!getIsPathDirectory(currPath)) {
            currPath = currPath.getParentPath()
        }

        // do not allow copying files in the recycle bin manually
        if (currPath.startsWith(filesDir.absolutePath)) {
            currPath = internalStoragePath
        }

        filepicker_placeholder.setTextColor(getProperTextColor())
        filepicker_fastscroller.updateColors(getProperPrimaryColor())

        updateToolbarText()
        tryUpdateItems()
    }

    private fun containsDirectory(items: List<AESDirItem>) = items.any { it.isDirectory }

    private fun tryUpdateItems() {
        ensureBackgroundThread {
            getItems(currPath) {
                runOnUiThread {
                    filepicker_placeholder.beGone()
                    updateItems(it as ArrayList<AESDirItem>)
                }
            }
        }
    }

    private fun getItems(path: String, callback: (List<FileDirItem>) -> Unit) {
        val lastModifieds = getFolderLastModifieds(path)
        getRegularItems(path, lastModifieds, callback)
    }

    private fun getRegularItems(path: String, lastModifieds: HashMap<String, Long>, callback: (List<FileDirItem>) -> Unit) {
        val items = ArrayList<FileDirItem>()
        val files = File(path).listFiles()?.filterNotNull()
        if (files == null) {
            callback(items)
            return
        }

        for (file in files) {
            val curPath = file.absolutePath
            val curName = curPath.getFilenameFromPath()
            val nameWithoutExt = file.nameWithoutExtension
            val parentPath = file.parent
            val size = file.length()
            val extn = ".${file.extension}"
            var lastModified = lastModifieds.remove(curPath)
            val isDirectory = if (lastModified != null) false else file.isDirectory
            if (lastModified == null) {
                lastModified = 0    // we don't actually need the real lastModified that badly, do not check file.lastModified()
            }

            if (isDirectory) {
                val children = file.getVaultDirChildrenCount()
                items.add(AESHelper.decryptAlbumData(AESDirItem(curPath, curName, isDirectory, children, size, lastModified)))
            } else if (extn.isExtVideo()) {
                if (file.extension == "sys") {
                    items.add(AESHelper.decryptMediaFileData(this, AESDirItem(curPath, curName, isDirectory, 0, size, lastModified).apply {
                        mInfoFile = File(parentPath, nameWithoutExt + AES_META_EXT)
                        mThumbFile = File(parentPath, nameWithoutExt + AES_THUMB_EXT)
                        encodedName = nameWithoutExt
                    }))
                } else {
                    items.add(AESDirItem(curPath, curName, isDirectory, 0, size, lastModified))
                }
            } else if (extn.isExtImage()) {
                if (extn == AES_IMAGE_EXT) {
                    items.add(AESHelper.decryptMediaFileData(this, AESDirItem(curPath, curName, isDirectory, 0, size, lastModified).apply {
                        mInfoFile = File(parentPath, nameWithoutExt + AES_META_EXT)
                        mThumbFile = File(parentPath, nameWithoutExt + AES_THUMB_EXT)
                        encodedName = nameWithoutExt
                    }))
                } else {
                    items.add(AESDirItem(curPath, curName, isDirectory, 0, size, lastModified))
                }
            }

        }
        callback(items)
    }

    private fun updateItems(items: ArrayList<AESDirItem>) {
//        if (!containsDirectory(items) && !mFirstUpdate) {
//            return
//        }
        list_empty_view.beVisibleIf(items.isEmpty())
        val sortedItems = items.sortedWith(AESDirItemComparator)
        adapter = AESFileAdapter(this, sortedItems, filepicker_list) {
            if ((it as FileDirItem).isDirectory) {
                setCurrentPath(it.path)
            } else {
                openMediaFile(it.path)
            }
        }

        val layoutManager = filepicker_list.layoutManager as LinearLayoutManager
        mScrollStates[mPrevPath.trimEnd('/')] = layoutManager.onSaveInstanceState()!!


        filepicker_list.adapter = adapter

        if (mPrevPath != currPath)
            filepicker_list.scheduleLayoutAnimation()

        layoutManager.onRestoreInstanceState(mScrollStates[currPath.trimEnd('/')])


        mFirstUpdate = false
        mPrevPath = currPath
    }

    private fun openMediaFile(path: String) {

        Intent(this, ViewPagerActivity::class.java).apply {
            putExtra(SKIP_AUTHENTICATION, intent.getBooleanExtra(SKIP_AUTHENTICATION, true))
            putExtra(SHOW_FAVORITES, intent.getBooleanExtra(SHOW_FAVORITES, false))
            putExtra(IS_VIEW_INTENT, true)
            putExtra(IS_FROM_GALLERY, true)
            putExtra(PATH, path)
            startActivity(this)
        }
    }

    private fun startEncryption(filePath: String) {
        AESHelper.tasker.showView(this)
        linePrint("starting work for $filePath")
        AESHelper.startEncryption(filePath, currPath)
    }
}

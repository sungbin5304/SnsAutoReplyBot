package com.sungbin.reply.bot.view.sourcehub.view.activity

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.shashank.sony.fancytoastlib.FancyToast
import com.sungbin.reply.bot.view.sourcehub.dto.BoardDataItem
import com.sungbin.reply.bot.view.sourcehub.utils.Utils

import kotlinx.android.synthetic.main.activity_board_view.*
import kotlinx.android.synthetic.main.activity_board_view.toolbar
import kotlinx.android.synthetic.main.content_board_view.*
import android.view.Gravity
import android.view.View
import android.webkit.WebView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.*
import com.mancj.slideup.SlideUp
import com.mancj.slideup.SlideUpBuilder
import com.sungbin.reply.bot.view.sourcehub.adapter.CommentListAdapter
import com.sungbin.reply.bot.view.sourcehub.dto.BoardActionItem
import com.sungbin.reply.bot.view.sourcehub.dto.CommentListItem
import com.sungbin.reply.bot.view.sourcehub.utils.DialogUtils
import com.sungbin.reply.bot.view.sourcehub.utils.FirebaseUtils
import com.sungbin.reply.bot.R
import com.sungbin.reply.bot.widget.LineNumberTextView
import kotlinx.android.synthetic.main.content_comment_page.*
import org.apache.commons.lang3.StringUtils
import java.lang.Exception
import kotlin.collections.ArrayList


class PostViewActivity : AppCompatActivity() {

    private var adapter: CommentListAdapter? = null
    private var items: ArrayList<CommentListItem>? = null
    private val reference = FirebaseDatabase.getInstance().reference
    private var slideUp: SlideUp? = null
    private var boardDataItem: BoardDataItem? = null
    private var isGood = false
    private var isBad = false
    private var actionKey: String? = null
    private val key = Utils.makeRandomUUID()
    private var viewer: WebView? = null

    @SuppressLint("ObsoleteSdkInt", "SetJavaScriptEnabled", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_board_view)
        toolbar.title = ""
        setSupportActionBar(toolbar)

        viewer = findViewById(R.id.viewer)

        val uid = Utils.readData(applicationContext, "uid", "null")!!
        val uuid = intent.getStringExtra("uuid")
        reference.child("Board").child(uuid).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                boardDataItem = dataSnapshot.getValue(BoardDataItem::class.java)

                @Suppress("DEPRECATION")
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    viewer!!.loadData(boardDataItem!!.content, "text/html", "UTF-8")
                } else {
                    viewer!!.loadData(boardDataItem!!.content, "text/html;", "charset=UTF-8")
                }

                toolbar_title.text = boardDataItem!!.title
                board_sender.text = boardDataItem!!.name
                board_desc.text = boardDataItem!!.desc
                board_good_count.text = boardDataItem!!.good_count.toString()
                board_bad_count.text = boardDataItem!!.bad_count.toString()
            }
            override fun onCancelled(databaseError: DatabaseError) {
                Utils.toast(applicationContext,
                    databaseError.message,
                    FancyToast.LENGTH_SHORT, FancyToast.ERROR)
            }
        })

        val webSettings = viewer!!.settings
        webSettings.builtInZoomControls = true
        webSettings.javaScriptEnabled = true
        webSettings.setSupportZoom(false)

        slideUp = SlideUpBuilder(view_comment_list)
            .withListeners(object : SlideUp.Listener.Events {
                override fun onSlide(percent: Float) {
                    viewer!!.alpha = percent / 100
                    if (comment.isShown && percent < 100) {
                        comment.hide()
                    }
                }

                override fun onVisibilityChanged(visibility: Int) {
                    if (visibility == View.GONE){
                        comment.show()
                    }
                }
            })
            .withStartGravity(Gravity.BOTTOM)
            .withStartState(SlideUp.State.HIDDEN)
            .withSlideFromOtherView(layout_post_view)
            .build()

        reference.child("User Action").child(uid).child("board_good")
            .addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                try {
                    val actionData = dataSnapshot.getValue(BoardActionItem::class.java)
                    isGood = actionData!!.uuid == uuid
                    if(isGood) {
                        board_good_count.setTypeface(null, Typeface.BOLD)
                        actionKey = actionData.key
                    }
                }
                catch (e: Exception) {
                    Utils.error(applicationContext,
                        e, "Load board_good listener.")
                }

            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {
            }

            override fun onCancelled(databaseError: DatabaseError) {
            }
        })

        reference.child("User Action").child(uid).child("board_bad")
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                    try {
                        val actionData = dataSnapshot.getValue(BoardActionItem::class.java)
                        isBad = actionData!!.uuid == uuid
                        if(isBad) {
                            board_bad_count.setTypeface(null, Typeface.BOLD)
                            actionKey = actionData.key
                        }
                    }
                    catch (e: Exception) {
                        Utils.error(applicationContext,
                            e, "Load board_bad listener.")
                    }

                }

                override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {

                }

                override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                }

                override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {
                }

                override fun onCancelled(databaseError: DatabaseError) {
                }
            })


        information_view.setOnClickListener {
            slideUp!!.hide()
        }

        frame_view.setOnClickListener {
            slideUp!!.hide()
        }

        main_view.setOnClickListener {
            slideUp!!.hide()
        }

        board_good.setOnClickListener {
            if(isGood) {
                Utils.toast(applicationContext,
                    getString(R.string.already_post_good),
                    FancyToast.LENGTH_SHORT, FancyToast.WARNING)
            }
            else {
                val good_count = (boardDataItem!!.good_count)?.plus(1)
                var bad_count = boardDataItem!!.bad_count
                if(isBad){
                    reference.child("User Action").child(uid)
                        .child("board_bad").child(actionKey!!).removeValue()
                    bad_count = bad_count?.minus(1)
                    board_bad_count.setTypeface(null, Typeface.NORMAL)
                }
                isBad = false
                isGood = true
                val data = BoardDataItem(
                    boardDataItem!!.title,
                    boardDataItem!!.desc, good_count, bad_count,
                    boardDataItem!!.source, boardDataItem!!.version,
                    boardDataItem!!.uuid, boardDataItem!!.name, boardDataItem!!.content
                )
                reference.child("Board").child(uuid).setValue(data)

                reference.child("User Action").child(uid)
                    .child("board_good").child(key).setValue(BoardActionItem(uuid, key))
            }
        }

        board_bad.setOnClickListener {
            if(isBad) {
                Utils.toast(applicationContext,
                    getString(R.string.already_post_bad),
                    FancyToast.LENGTH_SHORT, FancyToast.WARNING)
            }
            else {
                val bad_count = (boardDataItem!!.bad_count)?.plus(1)
                var good_count = boardDataItem!!.good_count
                if(isGood){
                    reference.child("User Action").child(uid)
                        .child("board_good").child(actionKey!!).removeValue()
                    good_count = good_count?.minus(1)
                    board_good_count.setTypeface(null, Typeface.NORMAL)
                }
                isBad = true
                isGood = false
                val data = BoardDataItem(
                    boardDataItem!!.title,
                    boardDataItem!!.desc, good_count, bad_count,
                    boardDataItem!!.source, boardDataItem!!.version,
                    boardDataItem!!.uuid, boardDataItem!!.name, boardDataItem!!.content
                )
                reference.child("Board").child(uuid).setValue(data)

                reference.child("User Action").child(uid)
                    .child("board_bad").child(key).setValue(BoardActionItem(uuid, key))
            }
        }

        comment.setOnClickListener {
            slideUp!!.show()
            toolbar_title.text = "${toolbar_title.text} - 댓글"
        }

        items = ArrayList()
        adapter = CommentListAdapter(items, this@PostViewActivity, uuid)
        list.layoutManager = LinearLayoutManager(applicationContext)
        list.adapter = adapter

        val commentItemCash: ArrayList<CommentListItem> = ArrayList()
        reference.child("Board Comment").child(uuid)
            .addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, s: String?) {
                try {
                    val commentDataItem = dataSnapshot.getValue(CommentListItem::class.java)
                    if(!commentItemCash.contains(commentDataItem)) {
                        items!!.add(commentDataItem!!)
                        commentItemCash.add(commentDataItem)
                        adapter!!.notifyDataSetChanged()
                    }
                }
                catch (e: Exception) {
                    Utils.error(applicationContext,
                        e, "Load comment list listener.")
                }
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, s: String?) {
                /*val commentDataItem = dataSnapshot.getValue(CommentListItem::class.java)
                Log.d("SSS", commentDataItem!!.comment)*/
                Utils.toast(applicationContext, "댓글이 수정되었습니다.\n게시글을 다시 로드하시면 반영됩니다.",
                    FancyToast.LENGTH_SHORT, FancyToast.SUCCESS)
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                /*val commentDataItem = dataSnapshot.getValue(CommentListItem::class.java)
                Log.d("TTT", commentDataItem!!.comment)
                //items!!.remove(commentDataItem)
                //commentItemCash.remove(commentDataItem)
                Log.d("SSS", items!!.contains(commentDataItem).toString())
                //adapter!!.deleteItem(items!!.indexOf(commentDataItem))
                adapter!!.notifyDataSetChanged()*/
                Utils.toast(applicationContext, "댓글이 삭제되었습니다.\n게시글을 다시 로드하시면 반영됩니다.",
                    FancyToast.LENGTH_SHORT, FancyToast.SUCCESS)
            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, s: String?) {

            }

            override fun onCancelled(databaseError: DatabaseError) {
                Utils.toast(applicationContext,
                    databaseError.message,
                    FancyToast.LENGTH_SHORT, FancyToast.ERROR)
            }
        })

        post_comment.setOnClickListener {
            val ctx = this@PostViewActivity

            val dialog = AlertDialog.Builder(ctx)
            dialog.setTitle(getString(R.string.post_comment))

            val layout = LinearLayout(ctx)
            layout.orientation = LinearLayout.VERTICAL

            val textview = TextView(ctx)
            textview.text = getString(R.string.max_length_one_fiive_zero)
            textview.gravity = Gravity.CENTER
            layout.addView(textview)

            val input = EditText(ctx)
            input.hint = getString(R.string.string_comment)
            input.filters = arrayOf(InputFilter.LengthFilter(150))
            layout.addView(input)

            dialog.setView(
                DialogUtils.makeMarginLayout(
                    resources,
                    ctx, layout
                )
            )
            dialog.setNegativeButton(getString(R.string.string_cancel), null)
            dialog.setPositiveButton(getString(R.string.post_complete)) { _, _ ->
                val comment = input.text.toString()
                if(StringUtils.isBlank(comment)){
                    Utils.toast(ctx,
                        getString(R.string.please_input_comment),
                        FancyToast.LENGTH_SHORT, FancyToast.WARNING)
                }
                else {
                    val data = CommentListItem(Utils.readData(applicationContext, "nickname", "User"),
                        comment, uuid, uid, key)
                    reference.child("Board Comment").child(uuid).child(key).setValue(data)
                    Utils.toast(applicationContext,
                        getString(R.string.comment_post_success),
                        FancyToast.LENGTH_SHORT, FancyToast.SUCCESS)
                    FirebaseUtils.showNoti(applicationContext,
                        getString(R.string.new_comment),
                        boardDataItem!!.title + "에 댓글이 작성되었습니다.",
                        "new_comment")
                }
            }
            dialog.show()
        }
    }

    override fun onBackPressed() {
        if(slideUp!!.isVisible) {
            slideUp!!.hide()
            toolbar_title.text = boardDataItem!!.title
        }
        else super.onBackPressed()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if(boardDataItem!!.source != "스크립트 업로드:V.000") {
            menu.add(0, 1, 0, getString(R.string.string_version_list))
                .setIcon(R.drawable.ic_history_white_24dp)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
            menu.add(0, 2, 0, getString(R.string.view_script))
                .setIcon(R.drawable.ic_code_white_24dp)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
        return true
    }

    @SuppressLint("SetTextI18n")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if(id == 1){
            val ctx = this@PostViewActivity

            val dialog = AlertDialog.Builder(ctx)
            dialog.setTitle(getString(R.string.input_version))

            val layout = LinearLayout(ctx)
            layout.orientation = LinearLayout.VERTICAL

            val textview = TextView(ctx)
            textview.text = getString(R.string.input_load_version)
            textview.gravity = Gravity.CENTER
            layout.addView(textview)

            val input = EditText(ctx)
            input.hint = getString(R.string.string_comment)
            input.inputType = 0x00000002
            input.filters = arrayOf(InputFilter.LengthFilter(3))
            layout.addView(input)

            dialog.setView(
                DialogUtils.makeMarginLayout(
                    resources,
                    ctx, layout
                )
            )
            dialog.setNegativeButton(getString(R.string.string_cancel), null)
            dialog.setPositiveButton(getString(R.string.string_load)) { _, _ ->
                val key = boardDataItem!!.source + ":V:" + input.text.toString()
                reference.child("Board").child(key).addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        try {
                            boardDataItem = dataSnapshot.getValue(BoardDataItem::class.java)
                            @Suppress("DEPRECATION")
                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                                viewer!!.loadData(boardDataItem!!.content, "text/html", "UTF-8")
                            } else {
                                viewer!!.loadData(boardDataItem!!.content, "text/html;", "charset=UTF-8")
                            }

                            toolbar_title.text = boardDataItem!!.title
                            board_sender.text = boardDataItem!!.name
                            board_desc.text = boardDataItem!!.desc
                            board_good_count.text = boardDataItem!!.good_count.toString()
                            board_bad_count.text = boardDataItem!!.bad_count.toString()

                            Utils.toast(
                                applicationContext,
                                getString(R.string.load_version_success),
                                FancyToast.LENGTH_SHORT, FancyToast.SUCCESS
                            )
                        }
                        catch(e: Exception){
                            Utils.toast(applicationContext!!,
                                getString(R.string.cant_load_version),
                                FancyToast.LENGTH_SHORT, FancyToast.WARNING)
                        }
                    }
                    override fun onCancelled(databaseError: DatabaseError) {
                        Utils.toast(applicationContext,
                            databaseError.message,
                            FancyToast.LENGTH_SHORT, FancyToast.ERROR)
                    }
                })
            }
            dialog.show()
        }

        if(id == 2){
            val ctx = this@PostViewActivity
            val dialog = AlertDialog.Builder(ctx)
            dialog.setTitle(boardDataItem!!.source!!.replace("js", ".js"))

            val layout = LinearLayout(ctx)
            layout.orientation = LinearLayout.VERTICAL

            val view = LineNumberTextView(ctx)
            view.text = "function test(){\nreturn \"\t\t\t\tThis is making function...\"\n}"
            layout.addView(view)

            dialog.setView(DialogUtils.makeMarginLayout(resources,
                ctx, layout))
            dialog.show()
            Utils.toast(ctx,
                getString(R.string.is_making),
                FancyToast.LENGTH_SHORT, FancyToast.INFO)
        }

        return super.onOptionsItemSelected(item)
    }

}

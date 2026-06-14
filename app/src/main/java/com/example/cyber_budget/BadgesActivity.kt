package com.example.cyber_budget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * BadgesActivity: Displays only the badges that the user has archived (earned).
 * Updated: Fixed scrolling and width to prevent explanation text from being cut off.
 */
class BadgesActivity : AppCompatActivity() {

    private val TAG = "Badges_Activity"
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    
    private lateinit var card1: View
    private lateinit var card2: View
    private lateinit var card3: View
    private lateinit var card4: View
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_badges)
        HeaderHelper.setupHeader(this)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        card1 = findViewById(R.id.card_badge_1)
        card2 = findViewById(R.id.card_badge_2)
        card3 = findViewById(R.id.card_badge_3)
        card4 = findViewById(R.id.card_badge_4)
        tvEmpty = findViewById(R.id.tv_empty_badges)

        val navContainer = findViewById<View>(R.id.include_nav)
        val mainLayout = findViewById<View>(R.id.main_badges)
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.setPadding(systemBars.left, 0, systemBars.right, 0)
            navContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = navBars.bottom
            }
            insets
        }

        setupNavbar()
        loadArchivedBadges()
        setupBadgeClickListeners()
    }

    private fun loadArchivedBadges() {
        val userId = auth.currentUser?.uid ?: return
        
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val archived = (doc.get("earnedBadges") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    displayArchivedBadges(archived)
                } else {
                    tvEmpty.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading badges", e)
                tvEmpty.visibility = View.VISIBLE
            }
    }

    private fun displayArchivedBadges(archived: List<String>) {
        var anyFound = false
        if (archived.contains("activelogger")) { card1.visibility = View.VISIBLE; anyFound = true }
        if (archived.contains("budgetpro")) { card2.visibility = View.VISIBLE; anyFound = true }
        if (archived.contains("financeguru")) { card3.visibility = View.VISIBLE; anyFound = true }
        if (archived.contains("saverhero")) { card4.visibility = View.VISIBLE; anyFound = true }
        tvEmpty.visibility = if (anyFound) View.GONE else View.VISIBLE
    }

    private fun setupNavbar() {
        findViewById<View>(R.id.ll_nav_profile)?.alpha = 1.0f
        findViewById<View>(R.id.ll_nav_home)?.setOnClickListener { navigateTo(MainActivity::class.java) }
        findViewById<View>(R.id.ll_nav_insights)?.setOnClickListener { navigateTo(SummaryActivity::class.java) }
        findViewById<View>(R.id.nav_plus_container)?.setOnClickListener { navigateTo(AddTransactionActivity::class.java, false) }
        findViewById<View>(R.id.ll_nav_activity)?.setOnClickListener { navigateTo(TransactionsActivity::class.java) }
    }

    private fun navigateTo(cls: Class<*>, reorder: Boolean = true) {
        val intent = Intent(this, cls)
        if (reorder) intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivity(intent)
    }

    private fun setupBadgeClickListeners() {
        card1.setOnClickListener {
            val iv = findViewById<ImageView>(R.id.iv_badge_image)
            showFullBadge(iv?.drawable, "Active Logger", 
                "Consistency is key! You earned this by logging 10 or more expenses. It shows your dedication to tracking every cent of your budget cycle.")
        }
        card2.setOnClickListener {
            val iv = findViewById<ImageView>(R.id.iv_badge_image_2)
            showFullBadge(iv?.drawable, "Budget Pro", 
                "Master of discipline! You've successfully managed your cycle with zero categories going over budget. Great job sticking to your plan!")
        }
        card3.setOnClickListener {
            val iv = findViewById<ImageView>(R.id.iv_badge_image_3)
            showFullBadge(iv?.drawable, "Finance Guru", 
                "Expert level budgeting! You've maintained strict control over at least 3 different spending categories simultaneously without overspending.")
        }
        card4.setOnClickListener {
            val iv = findViewById<ImageView>(R.id.iv_badge_image_4)
            showFullBadge(iv?.drawable, "Saver Hero", 
                "Wealth builder! Your total income for this cycle was higher than your total expenses, meaning you've successfully saved money.")
        }
    }

    private fun showFullBadge(drawable: Drawable?, title: String, description: String) {
        val rootContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1A1A2E"))
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f)
            isFillViewport = true
        }

        val innerContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 60, 40, 60)
        }

        val imageSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 160f, resources.displayMetrics).toInt()
        val fullIv = ImageView(this).apply {
            setImageDrawable(drawable)
            layoutParams = LinearLayout.LayoutParams(imageSize, imageSize).apply { setMargins(0, 0, 0, 40) }
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        val tvTitle = TextView(this).apply {
            text = title
            textSize = 24f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }

        val tvDesc = TextView(this).apply {
            text = description
            textSize = 16f
            setTextColor(Color.parseColor("#CCCCCC"))
            gravity = Gravity.CENTER
            setLineSpacing(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4f, resources.displayMetrics), 1.1f)
        }

        innerContainer.addView(fullIv)
        innerContainer.addView(tvTitle)
        innerContainer.addView(tvDesc)
        scrollView.addView(innerContainer)
        rootContainer.addView(scrollView)

        val dialog = AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_NoActionBar)
            .setView(rootContainer)
            .setPositiveButton("Awesome!", null)
            .create()

        dialog.show()

        // Force a wider dialog and handle height wrap
        dialog.window?.let { window ->
            val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }
}

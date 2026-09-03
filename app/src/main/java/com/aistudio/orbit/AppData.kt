package com.aistudio.orbit

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

enum class AppLanguage { FA, EN }

data class AppStrings(
    val appName: String,
    val authorInfo: String,
    val trackerTab: String,
    val tasksTab: String,
    val seedAddress: String,
    val depth: String,
    val limit: String,
    val topNodes: String,
    val crawl: String,
    val newCrawl: String,
    val saveGraph: String,
    val saveFormat: String,
    val graphSavedMsg: String,
    val taskSummary: String,
    val completed: String,
    val pending: String,
    val addTask: String,
    val taskName: String,
    val category: String,
    val filterCategory: String,
    val allCategories: String,
    val starting: String,
    val errorProvideSeed: String,
    val errorSaving: String,
    val doneWallets: String,
    
    // New fields
    val priority: String,
    val low: String,
    val medium: String,
    val high: String,
    val dueDate: String,
    val noDueDate: String,
    val approaching: String,
    val overdue: String,
    val selectDate: String,
    val searchPlaceholder: String,
    val taskDesc: String,
    val infoTitle: String,
    
    // Guide popups
    val guideSeeds: String,
    val guideDepth: String,
    val guideLimit: String,
    val guideTopNodes: String,
    val guideTaskName: String,
    val guideCategory: String,
    val guidePriority: String,
    val guideDueDate: String,
    val guideDesc: String,
    
    // PDF and Saved Search history strings
    val exportPdf: String,
    val saveSearch: String,
    val searchHistory: String,
    val noHistory: String,
    val loadGraph: String,
    val deleteBtn: String,
    val savedSuccess: String,
    val pdfSaved: String,
    val addNote: String,
    val searchHistoryPlaceholder: String,
    val historyTitle: String,
    val closeBtn: String,
    val moreDetailsBtn: String,
    val moreDetailsContent: String
)

val stringsFa = AppStrings(
    appName = "ابزار ردیابی تراکنش ارز دیجیتال",
    authorInfo = "نویسنده: MSKPG-3916",
    trackerTab = "ردیاب تراکنش",
    tasksTab = "وظایف",
    seedAddress = "آدرس‌های پایه (با کاما جدا کنید)",
    depth = "عمق جستجو",
    limit = "محدودیت هر صفحه",
    topNodes = "تعداد گره‌های برتر",
    crawl = "شروع ردیابی",
    newCrawl = "ردیابی جدید",
    saveGraph = "ذخیره گراف",
    saveFormat = "فرمت ذخیره",
    graphSavedMsg = "فایل با موفقیت ذخیره شد در پوشه دانلودها:",
    taskSummary = "خلاصه وظایف",
    completed = "تکمیل شده",
    pending = "در انتظار",
    addTask = "افزودن وظیفه",
    taskName = "نام وظیفه",
    category = "دسته‌بندی",
    filterCategory = "فیلتر دسته‌بندی",
    allCategories = "همه",
    starting = "در حال شروع...",
    errorProvideSeed = "خطا: لطفاً حداقل یک آدرس پایه وارد کنید.",
    errorSaving = "خطا در ذخیره فایل",
    doneWallets = "پایان. کیف پول‌ها: %d، اتصالات: %d",
    
    // New fields
    priority = "اولویت",
    low = "کم",
    medium = "متوسط",
    high = "زیاد",
    dueDate = "تاریخ سررسید",
    noDueDate = "بدون تاریخ",
    approaching = "نزدیک به موعد",
    overdue = "گذشته از موعد",
    selectDate = "انتخاب تاریخ",
    searchPlaceholder = "جستجو در عنوان یا توضیحات وظایف...",
    taskDesc = "توضیحات وظیفه",
    infoTitle = "راهنمای فیلد ورودی",
    
    // Guide popups
    guideSeeds = "آدرس یا آدرس‌های کیف پول بیت‌کوین را برای شروع ردیابی وارد کنید. آدرس‌های متعدد را با کاما جدا کنید.",
    guideDepth = "میزان عمق جستجوی اتصالات. مقادیر بزرگتر به صورت عمیق‌تر ردیابی می‌کنند اما زمان بیشتری نیاز دارند.",
    guideLimit = "حداکثر تعداد تراکنش‌ها در هر صفحه/درخواست. از استفاده بیش از حد از API جلوگیری می‌کند.",
    guideTopNodes = "تعداد فعال‌ترین و متصل‌ترین گره‌های کیف پول جهت نمایش در گراف خروجی.",
    guideTaskName = "عنوان یا نام وظیفه‌ای که می‌خواهید ذخیره و ردیابی کنید.",
    guideCategory = "دسته‌بندی وظایف (مانند شخصی، کاری، ارزی) جهت سازماندهی بهتر.",
    guidePriority = "میزان اهمیت و اولویت انجام کار: کم، متوسط، زیاد.",
    guideDueDate = "تاریخ مهلت انجام کار. وظایف نزدیک به سررسید یا تاخیرخورده با هشدار نمایش داده می‌شوند.",
    guideDesc = "جزئیات، توضیحات یا یادداشت‌های اضافی مربوط به این کار.",
    
    exportPdf = "خروجی گزارش PDF",
    saveSearch = "ذخیره نتیجه جستجو",
    searchHistory = "تاریخچه ردیابی‌های ذخیره شده (شمسی)",
    noHistory = "هیچ موردی در تاریخچه یافت نشد.",
    loadGraph = "بارگذاری گراف",
    deleteBtn = "حذف",
    savedSuccess = "ردیابی با موفقیت در تاریخچه محلی (شمسی) ذخیره شد.",
    pdfSaved = "گزارش PDF با موفقیت در پوشه دانلودها ذخیره شد:",
    addNote = "یادداشت برای این جستجو",
    searchHistoryPlaceholder = "جستجو در تاریخچه با آدرس، تاریخ یا یادداشت...",
    historyTitle = "تاریخچه جستجوهای ردیابی",
    closeBtn = "بستن",
    moreDetailsBtn = "جزئیات بیشتر",
    moreDetailsContent = "این پروژه با الهام از پیشنهاد، اعتماد و حمایتهای ارزشمند استاد بزرگوار، جناب آقای باهنر آغاز شد و تحت نظارت تخصصی ایشان مراحل معماری نرمافزاری و توسعه فنی توسط اینجانب انجام و در حال بهبود و اصلاح میباشد.\nاز همراهی، راهنماییها و اعتماد ایشان که زمینه شکلگیری این سامانه را فراهم کرد، صمیمانه سپاسگزارم.\n\nس.خ.پ"
)

val stringsEn = AppStrings(
    appName = "Cryptocurrency tracking tool",
    authorInfo = "Author: MSKPG-3916",
    trackerTab = "Tracker",
    tasksTab = "Tasks",
    seedAddress = "Seed Address(es) comma separated",
    depth = "Depth",
    limit = "Limit",
    topNodes = "Top Nodes",
    crawl = "Crawl",
    newCrawl = "New Crawl",
    saveGraph = "Save Graph",
    saveFormat = "Format",
    graphSavedMsg = "Saved successfully to Downloads:",
    taskSummary = "Tasks Summary",
    completed = "Completed",
    pending = "Pending",
    addTask = "Add Task",
    taskName = "Task Name",
    category = "Category",
    filterCategory = "Filter by Category",
    allCategories = "All Categories",
    starting = "Starting...",
    errorProvideSeed = "Error: Please provide at least one seed address.",
    errorSaving = "Error saving file",
    doneWallets = "Done. Wallets: %d, Connections: %d",
    
    // New fields
    priority = "Priority",
    low = "Low",
    medium = "Medium",
    high = "High",
    dueDate = "Due Date",
    noDueDate = "No Due Date",
    approaching = "Approaching",
    overdue = "Overdue",
    selectDate = "Select Date",
    searchPlaceholder = "Search tasks by title or description...",
    taskDesc = "Description",
    infoTitle = "Input Field Guidance",
    
    // Guide popups
    guideSeeds = "Enter Bitcoin wallet address(es) to start tracking. Separate multiple addresses with commas.",
    guideDepth = "The depth of connection search. Higher values scan deeper but take more time.",
    guideLimit = "Maximum number of transactions to query per page/request. Prevents heavy API usage.",
    guideTopNodes = "The number of most connected wallet nodes to display in the visualization graph.",
    guideTaskName = "The title of the task you want to remember or track.",
    guideCategory = "Group your tasks under a category (e.g., Work, Personal, Crypto).",
    guidePriority = "Urgency level of the task: Low, Medium, or High.",
    guideDueDate = "Select a due date. Overdue or approaching tasks will show warnings.",
    guideDesc = "Additional details or notes about the task.",
    
    exportPdf = "Export PDF Report",
    saveSearch = "Save Search Result",
    searchHistory = "Saved Searches & History (Jalali)",
    noHistory = "No saved searches found.",
    loadGraph = "Load Graph",
    deleteBtn = "Delete",
    savedSuccess = "Search result saved successfully with Persian Date.",
    pdfSaved = "PDF report exported successfully to Downloads:",
    addNote = "Notes for this search",
    searchHistoryPlaceholder = "Search history by address, date or notes...",
    historyTitle = "Tracking Search History",
    closeBtn = "Close",
    moreDetailsBtn = "More Details",
    moreDetailsContent = "This project was initiated under the valuable guidance, trust, and support of our esteemed professor, Mr. Bahonar. Under his specialized supervision, the software architecture and technical development stages were carried out by me and are continuously being improved and refined.\nI would like to express my sincere gratitude for his collaboration, guidance, and trust, which laid the foundation for the creation of this platform.\n\nS. Kh. P."
)

@Serializable
data class TaskItem(
    val id: String,
    val name: String,
    val category: String,
    val isCompleted: Boolean,
    val order: Int,
    val dueDate: String? = null,
    val priority: String = "Medium",
    val description: String = ""
)

class TaskRepository(context: Context) {
    private val prefs = context.getSharedPreferences("tasks_prefs", Context.MODE_PRIVATE)
    private val TASKS_KEY = "tasks_json"

    fun saveTasks(tasks: List<TaskItem>) {
        val jsonString = Json.encodeToString(tasks)
        prefs.edit().putString(TASKS_KEY, jsonString).apply()
    }

    fun loadTasks(): List<TaskItem> {
        val jsonString = prefs.getString(TASKS_KEY, null) ?: return emptyList()
        return try {
            Json.decodeFromString<List<TaskItem>>(jsonString).sortedBy { it.order }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

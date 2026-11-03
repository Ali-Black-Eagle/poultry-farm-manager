package com.example.poultryapp.ui.screens.golden_breeding.components

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.poultryapp.data.model.GoldenBirdType

/**
 * تم‌های بصری و هنری منحصربه‌فرد برای نقشه بازی هر پرنده
 */
data class BirdArtTheme(
    val birdType: GoldenBirdType,
    val environmentTitleFa: String,
    val environmentTagFa: String,
    val backgroundBrush: Brush,
    val headerSurfaceColor: Color,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val nodePassedBrush: Brush,
    val nodeActiveGlow: Color,
    val cobblestoneColor: Color,
    val particleEmojis: List<String>,
    val summitTrophyTitleFa: String
) {
    companion object {
        fun getThemeForBird(birdType: GoldenBirdType): BirdArtTheme {
            return when (birdType) {
                // ۱. مرغ بومی و تخم‌گذار: مزرعه آفتابی و گل‌های آفتابگردان
                GoldenBirdType.LAYER_HEN -> BirdArtTheme(
                    birdType = birdType,
                    environmentTitleFa = "دشت سرسبز و مزرعه آفتابی",
                    environmentTagFa = "🌻 مزرعه آفتابگردان",
                    backgroundBrush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0A2912), // افق سپیده‌دم مزرعه
                            Color(0xFF144D22), // تپه‌های سرسبز بالادست
                            Color(0xFF1E6B31), // مرتع آفتابگردان
                            Color(0xFF165225), // چمنزار انبوه
                            Color(0xFF0D3316)  // خاک حاصلخیز پایینی
                        )
                    ),
                    headerSurfaceColor = Color(0xFF092410),
                    primaryAccent = Color(0xFFF59E0B),
                    secondaryAccent = Color(0xFF10B981),
                    nodePassedBrush = Brush.verticalGradient(listOf(Color(0xFF34D399), Color(0xFF059669))),
                    nodeActiveGlow = Color(0xFF4ADE80),
                    cobblestoneColor = Color(0xFFFFF8DC),
                    particleEmojis = listOf("🌻", "🌾", "🦋", "✨", "🍃"),
                    summitTrophyTitleFa = "اوج تخم‌گذاری و درآمد پایدار 🏆"
                )

                // ۲. مرغ گوشتی: فارم صنعتی کهربایی و مدرن
                GoldenBirdType.BROILER -> BirdArtTheme(
                    birdType = birdType,
                    environmentTitleFa = "فارم صنعتی پیشرفته و پرواربندی فشرده",
                    environmentTagFa = "⚡ سالن تهویه مدرن",
                    backgroundBrush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF180A04), // سقف فولادی سالن
                            Color(0xFF381404), // خطوط نئونی صنعتی
                            Color(0xFF5A2006), // کهربای سنسوریک
                            Color(0xFF3A1505), // بدنه مگافارم
                            Color(0xFF1A0A03)  // کفپوش شیاردار بتنی
                        )
                    ),
                    headerSurfaceColor = Color(0xFF1E0A03),
                    primaryAccent = Color(0xFFEA580C),
                    secondaryAccent = Color(0xFFFBBF24),
                    nodePassedBrush = Brush.verticalGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706))),
                    nodeActiveGlow = Color(0xFFFDE047),
                    cobblestoneColor = Color(0xFFFEF9C3),
                    particleEmojis = listOf("✨", "🌾", "⭐", "🍃", "💫"),
                    summitTrophyTitleFa = "پایان دوره پرواربندی ۴۵ روزه 🏆"
                )

                // ۳. بلدرچین: باغچه مینیاتوری یاسی و رازآلود
                GoldenBirdType.QUAIL -> BirdArtTheme(
                    birdType = birdType,
                    environmentTitleFa = "باغچه مینیاتوری و آشیانه یاسی",
                    environmentTagFa = "🌸 بهشت مینیاتوری",
                    backgroundBrush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF18042B), // آسمان گرگ‌ومیش فوجی
                            Color(0xFF2E094F), // مه بنفش رویایی
                            Color(0xFF4C107E), // ارغوان شکوفه‌های گیلاس
                            Color(0xFF320B54), // باغ سنگ‌های ذن
                            Color(0xFF150426)  // سایه شبانه باغچه
                        )
                    ),
                    headerSurfaceColor = Color(0xFF19052D),
                    primaryAccent = Color(0xFF06B6D4),
                    secondaryAccent = Color(0xFFEC4899),
                    nodePassedBrush = Brush.verticalGradient(listOf(Color(0xFFA855F7), Color(0xFF7C3AED))),
                    nodeActiveGlow = Color(0xFFC084FC),
                    cobblestoneColor = Color(0xFFFAF5FF),
                    particleEmojis = listOf("✨", "🌸", "⭐", "🍃", "💫"),
                    summitTrophyTitleFa = "آغاز تخم‌گذاری در ۴۰ روزگی 🏆"
                )

                // ۴. بوقلمون: دشت پاییزی بلوط و پرهای باشکوه
                GoldenBirdType.TURKEY -> BirdArtTheme(
                    birdType = birdType,
                    environmentTitleFa = "دشت پهناور درختان بلوط پاییزی",
                    environmentTagFa = "🍂 مرتع بلوط شاهانه",
                    backgroundBrush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1C0702), // غروب دشت بلوط
                            Color(0xFF381005), // تپه‌های شرابی خزان
                            Color(0xFF5E1B09), // پرهای شاهانه طلایی
                            Color(0xFF3D1105), // بیشه بلوط سرخ
                            Color(0xFF1A0702)  // برگ‌های ریخته بستر
                        )
                    ),
                    headerSurfaceColor = Color(0xFF1F0803),
                    primaryAccent = Color(0xFFD97706),
                    secondaryAccent = Color(0xFF84CC16),
                    nodePassedBrush = Brush.verticalGradient(listOf(Color(0xFFEA580C), Color(0xFF9A3412))),
                    nodeActiveGlow = Color(0xFFFB923C),
                    cobblestoneColor = Color(0xFFFFF7ED),
                    particleEmojis = listOf("✨", "🍂", "🍁", "🌾", "⭐"),
                    summitTrophyTitleFa = "رسیدن به وزن سنگین بازاریابی 🏆"
                )

                // ۵. اردک و غاز: تالاب فیروزه‌ای نیلوفرهای آبی
                GoldenBirdType.WATERFOWL -> BirdArtTheme(
                    birdType = birdType,
                    environmentTitleFa = "تالاب فیروزه‌ای و جزیره نیلوفرهای آبی",
                    environmentTagFa = "🪷 تالاب نیلوفرهای آبی",
                    backgroundBrush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF03192B), // ژرفای تاریک برکه
                            Color(0xFF073656), // فیروزه‌ای بلورین آب
                            Color(0xFF0B5282), // انعکاس مهتاب بر امواج
                            Color(0xFF083C61), // نیلوفرهای شناور
                            Color(0xFF021727)  // بستر زلال رودخانه
                        )
                    ),
                    headerSurfaceColor = Color(0xFF031C30),
                    primaryAccent = Color(0xFF14B8A6),
                    secondaryAccent = Color(0xFF38BDF8),
                    nodePassedBrush = Brush.verticalGradient(listOf(Color(0xFF06B6D4), Color(0xFF0284C7))),
                    nodeActiveGlow = Color(0xFF67E8F9),
                    cobblestoneColor = Color(0xFFF0FDF4),
                    particleEmojis = listOf("✨", "💧", "🫧", "🌿", "🪷"),
                    summitTrophyTitleFa = "بلوغ کامل گله آبزی و چراگری 🏆"
                )

                // ۶. عروس هلندی: آشیانه بهشتی و شاخه‌های سبز طوطی‌سانان
                GoldenBirdType.COCKATIEL -> BirdArtTheme(
                    birdType = birdType,
                    environmentTitleFa = "آشیانه بهشتی و شاخه‌های سبز طوطی‌سانان",
                    environmentTagFa = "🦜 آشیانه سبز طوطی‌سانان",
                    backgroundBrush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF02241C), // تاریکی بارانی جنگل
                            Color(0xFF044535), // سایبان برگ‌های نخل
                            Color(0xFF066950), // درخشش زمردی آشیانه
                            Color(0xFF044837), // شاخه‌های تاک استوایی
                            Color(0xFF021E17)  // بستر مرطوب استوایی
                        )
                    ),
                    headerSurfaceColor = Color(0xFF02241C),
                    primaryAccent = Color(0xFFFBBF24),
                    secondaryAccent = Color(0xFFF59E0B),
                    nodePassedBrush = Brush.verticalGradient(listOf(Color(0xFFFDE047), Color(0xFFEAB308))),
                    nodeActiveGlow = Color(0xFFFEF08A),
                    cobblestoneColor = Color(0xFFFEF08A),
                    particleEmojis = listOf("✨", "🍃", "⭐", "🌿", "💛"),
                    summitTrophyTitleFa = "استادی تکثیر و پرورش عروس هلندی 🏆"
                )

                // ۷. کبک چوکار: صخره‌های شیب‌دار زاگرس و دیواره‌های کوهستانی
                GoldenBirdType.PARTRIDGE -> BirdArtTheme(
                    birdType = birdType,
                    environmentTitleFa = "صخره‌های شیب‌دار زاگرس و دیواره‌های کوهستانی",
                    environmentTagFa = "⛰️ صخره‌های زاگرس و البرز",
                    backgroundBrush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF090E17), // قله‌های سنگی نیمه‌شب
                            Color(0xFF131E2E), // دیواره‌های صخره‌ای آلپی
                            Color(0xFF22344D), // شفق سرخ زاگرس
                            Color(0xFF172538), // سنگ‌ریزه‌های شیب‌دار
                            Color(0xFF080D15)  // دره عمیق کوهستان
                        )
                    ),
                    headerSurfaceColor = Color(0xFF0A0F1A),
                    primaryAccent = Color(0xFFEF4444),
                    secondaryAccent = Color(0xFFF97316),
                    nodePassedBrush = Brush.verticalGradient(listOf(Color(0xFFEF4444), Color(0xFFB91C1C))),
                    nodeActiveGlow = Color(0xFFF87171),
                    cobblestoneColor = Color(0xFFF1F5F9),
                    particleEmojis = listOf("✨", "🍃", "⭐", "🌾", "💫"),
                    summitTrophyTitleFa = "قهرمانی پرواربندی کبک چوکار 🏆"
                )

                // ۸. شترمرغ: دشت پهناور سافاری و رمل‌های کویری
                GoldenBirdType.OSTRICH -> BirdArtTheme(
                    birdType = birdType,
                    environmentTitleFa = "دشت پهناور سافاری و رمل‌های کویری",
                    environmentTagFa = "🏜️ پهنه آفتابی سافاری",
                    backgroundBrush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF240701), // سراب سوزان کویر
                            Color(0xFF451103), // رمل‌های مواج ماسه‌ای
                            Color(0xFF701E06), // خورشید تابان صحرا
                            Color(0xFF4A1303), // افق بی‌انتهای سافاری
                            Color(0xFF1C0501)  // ماسه‌های سرد شامگاهی
                        )
                    ),
                    headerSurfaceColor = Color(0xFF290802),
                    primaryAccent = Color(0xFFF59E0B),
                    secondaryAccent = Color(0xFFD97706),
                    nodePassedBrush = Brush.verticalGradient(listOf(Color(0xFFF97316), Color(0xFFC2410C))),
                    nodeActiveGlow = Color(0xFFFDBA74),
                    cobblestoneColor = Color(0xFFFFFBEB),
                    particleEmojis = listOf("✨", "🌾", "☀️", "⭐", "💫"),
                    summitTrophyTitleFa = "سرپرست ارشد مگافارم شترمرغ 🏆"
                )

                // ۹. مرغ شاخدار: باغستان مرواریدی، سایبان درختان و شکارگاه آفات
                GoldenBirdType.GUINEA_FOWL -> BirdArtTheme(
                    birdType = birdType,
                    environmentTitleFa = "باغستان درختان میوه و علفزار سرسبز مرواریدی",
                    environmentTagFa = "🪶 ساوانای مرغ شاخدار",
                    backgroundBrush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0A1813), // غروب بنفش و فیروزه‌ای ساوانا
                            Color(0xFF0F362A), // سایبان درختان اقاقیا
                            Color(0xFF165241), // کهکشان مرواریدی پرها
                            Color(0xFF103D30), // علفزار شبانه آفریقا
                            Color(0xFF071410)  // خاک تیره دشتستان
                        )
                    ),
                    headerSurfaceColor = Color(0xFF081A14),
                    primaryAccent = Color(0xFF0284C7),
                    secondaryAccent = Color(0xFF10B981),
                    nodePassedBrush = Brush.verticalGradient(listOf(Color(0xFF10B981), Color(0xFF047857))),
                    nodeActiveGlow = Color(0xFF34D399),
                    cobblestoneColor = Color(0xFFECFDF5),
                    particleEmojis = listOf("✨", "🪶", "🍃", "⭐", "💫"),
                    summitTrophyTitleFa = "قهرمان گله‌داری مرغ شاخدار و نگهبان باغستان 🏆"
                )

                // ۱۰. قناری: باغ مرکبات آفتابی، شکوفه‌های بهار و نغمه‌سرایی
                GoldenBirdType.CANARY -> BirdArtTheme(
                    birdType = birdType,
                    environmentTitleFa = "باغ مرکبات آفتابی، شکوفه‌های بهار و نغمه‌سرایی",
                    environmentTagFa = "🍋 باغ بهاری مرکبات",
                    backgroundBrush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF1C1A04), // طلایی زیتونی تیره
                            Color(0xFF383307), // لیمویی کهربایی عمیق
                            Color(0xFF5A520B), // زرد طلایی آفتابی
                            Color(0xFF3D3706), // سایه‌روشن باغ بهاری
                            Color(0xFF1A1803)  // بستر سایه‌دار خاک باغ
                        )
                    ),
                    headerSurfaceColor = Color(0xFF1E1A04),
                    primaryAccent = Color(0xFFFACC15),
                    secondaryAccent = Color(0xFFA3E635),
                    nodePassedBrush = Brush.verticalGradient(listOf(Color(0xFFFDE047), Color(0xFFEAB308))),
                    nodeActiveGlow = Color(0xFFFEF08A),
                    cobblestoneColor = Color(0xFFFEFCE8),
                    particleEmojis = listOf("🍋", "🎵", "✨", "🌸", "🎶"),
                    summitTrophyTitleFa = "جام بلورین آوازه‌خوانی و تکثیر رسمی قناری 🏆"
                )

                // ۱۱. کبوتر: برج کبوترخانه تاریخی، گنبدهای فیروزه‌ای و اوج آسمان
                GoldenBirdType.PIGEON -> BirdArtTheme(
                    birdType = birdType,
                    environmentTitleFa = "برج کبوترخانه تاریخی، گنبدهای فیروزه‌ای و اوج آسمان",
                    environmentTagFa = "🕊️ کبوترخانه کهن خشتی",
                    backgroundBrush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F172A), // گرگ و میش آسمان کویری
                            Color(0xFF1E293B), // سنگ‌های دودی برج کبوترخانه
                            Color(0xFF2E3A4D), // آبی لاجوردی و کاشی‌کاری
                            Color(0xFF1E2533), // آجرهای خشتی کهن
                            Color(0xFF0A0F1D)  // پایه‌های مستحکم برج
                        )
                    ),
                    headerSurfaceColor = Color(0xFF0F172A),
                    primaryAccent = Color(0xFF38BDF8),
                    secondaryAccent = Color(0xFFA855F7),
                    nodePassedBrush = Brush.verticalGradient(listOf(Color(0xFF38BDF8), Color(0xFF0284C7))),
                    nodeActiveGlow = Color(0xFF7DD3FC),
                    cobblestoneColor = Color(0xFFF0F9FF),
                    particleEmojis = listOf("🕊️", "✨", "🪶", "🏛️", "⭐"),
                    summitTrophyTitleFa = "مدال زرین مسافت بلند و بازگشت شگفت‌انگیز کبوتر 🏆"
                )

                // ۱۲. اردک مسکویی: تالاب نیلوفرهای آبی، نیزارهای وحشی و برکه گرگ‌ومیش
                GoldenBirdType.MUSCOVY_DUCK -> BirdArtTheme(
                    birdType = birdType,
                    environmentTitleFa = "تالاب نیلوفرهای آبی، نیزارهای وحشی و بیشه‌زار تالابی",
                    environmentTagFa = "🪷 آب‌بند و تالاب نیزار",
                    backgroundBrush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF042F2E), // زمردی تیره اعماق تالاب
                            Color(0xFF064E3B), // سبز یشمی نیزارها
                            Color(0xFF0F766E), // فیروزه‌ای خزه مرداب
                            Color(0xFF115E59), // آب‌های زلال انعکاسی
                            Color(0xFF022C22)  // بستر غنی تالابی
                        )
                    ),
                    headerSurfaceColor = Color(0xFF042F2E),
                    primaryAccent = Color(0xFF14B8A6), // فیروزه‌ای تالاب
                    secondaryAccent = Color(0xFFEF4444), // قرمز آتشین کارونکل صورت
                    nodePassedBrush = Brush.verticalGradient(listOf(Color(0xFF2DD4BF), Color(0xFF0D9488))),
                    nodeActiveGlow = Color(0xFF5EEAD4),
                    cobblestoneColor = Color(0xFFCCFBF1),
                    particleEmojis = listOf("🪷", "🦆", "🌾", "✨", "🍃"),
                    summitTrophyTitleFa = "جام زرین استادی پرورش ارگانیک اردک مسکویی 🏆"
                )
            }
        }

        /**
         * تم بصری اختصاصی و اشرافی برای فصل دوم (بورد فوق‌حرفه‌ای و مگافارمر)
         */
        fun getProThemeForBird(birdType: GoldenBirdType): BirdArtTheme {
            return BirdArtTheme(
                birdType = birdType,
                environmentTitleFa = "کلینیک فوق‌تخصصی و بورد پیشرفته ${birdType.titleFa}",
                environmentTagFa = "👑 بورد فوق‌حرفه‌ای دامپزشکی و مگافارم",
                backgroundBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF030712), // سیاه ابریشمی عمیق (Obsidian)
                        Color(0xFF0F172A), // زغالی نیمه‌شب
                        Color(0xFF1E1B4B), // سرمه‌ای سلطنتی ایندیگو
                        Color(0xFF172554), // آبی نفتی بورد
                        Color(0xFF020617)  // تاریک اعماق فضا
                    )
                ),
                headerSurfaceColor = Color(0xFF030712),
                primaryAccent = Color(0xFFF59E0B), // طلای اشرافی (Amber Gold)
                secondaryAccent = Color(0xFF38BDF8), // نئون فیروزه‌ای الکتریک
                nodePassedBrush = Brush.verticalGradient(
                    listOf(Color(0xFFFBBF24), Color(0xFFD97706), Color(0xFFB45309))
                ),
                nodeActiveGlow = Color(0xFFFDE047),
                cobblestoneColor = Color(0xFFFEF08A),
                particleEmojis = listOf("✨", "⭐", "👑", "💫"),
                summitTrophyTitleFa = "دیپلم افتخار بورد فوق‌تخصصی بین‌المللی 👑🏆"
            )
        }
    }
}


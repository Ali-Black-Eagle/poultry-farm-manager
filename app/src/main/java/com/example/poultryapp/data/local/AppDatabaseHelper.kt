package com.example.poultryapp.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.poultryapp.data.model.BirdSpecies
import com.example.poultryapp.data.model.ProductionType
import com.example.poultryapp.engine.ChargeItemType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class AppDatabaseHelper private constructor(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "poultry_farm.db"
        const val DATABASE_VERSION = 13

        @Volatile
        private var instance: AppDatabaseHelper? = null

        fun getInstance(context: Context): AppDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: AppDatabaseHelper(context.applicationContext).also { instance = it }
            }
        }
    }

    private val _dbUpdateSignal = MutableStateFlow(0L)
    val dbUpdateSignal: StateFlow<Long> = _dbUpdateSignal.asStateFlow()

    private fun notifyDataChanged() {
        _dbUpdateSignal.value = System.currentTimeMillis()
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE flocks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                species TEXT NOT NULL,
                breed TEXT NOT NULL,
                birth_epoch_millis INTEGER NOT NULL,
                bird_count INTEGER NOT NULL,
                photo_uri TEXT,
                notes TEXT,
                created_at_millis INTEGER NOT NULL,
                subgroups_json TEXT,
                is_feed_enabled INTEGER NOT NULL DEFAULT 1,
                is_water_enabled INTEGER NOT NULL DEFAULT 1,
                custom_temp_celsius REAL
            );
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE feed_water_charges (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                flock_id INTEGER NOT NULL,
                item_type TEXT NOT NULL,
                charged_amount REAL NOT NULL,
                charge_timestamp_millis INTEGER NOT NULL,
                date_jalali_str TEXT NOT NULL,
                notes TEXT,
                FOREIGN KEY(flock_id) REFERENCES flocks(id) ON DELETE CASCADE
            );
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE calendar_schedules (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                flock_id INTEGER,
                flock_name TEXT,
                schedule_type TEXT NOT NULL,
                title TEXT NOT NULL,
                notes TEXT,
                days_of_week_csv TEXT NOT NULL,
                hour INTEGER NOT NULL,
                minute INTEGER NOT NULL,
                is_enabled INTEGER NOT NULL DEFAULT 1,
                schedule_mode TEXT NOT NULL DEFAULT 'WEEKLY',
                day_of_month INTEGER,
                FOREIGN KEY(flock_id) REFERENCES flocks(id) ON DELETE CASCADE
            );
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE warehouse_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                category TEXT NOT NULL,
                current_stock REAL NOT NULL,
                unit TEXT NOT NULL,
                price_per_unit REAL NOT NULL DEFAULT 0.0,
                assigned_flock_ids_csv TEXT,
                min_alert_threshold REAL NOT NULL DEFAULT 50.0,
                is_for_sale INTEGER NOT NULL DEFAULT 0,
                notes TEXT,
                updated_at_millis INTEGER NOT NULL
            );
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE app_settings (
                setting_key TEXT PRIMARY KEY,
                setting_value TEXT NOT NULL
            );
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS custom_breeds (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                species TEXT NOT NULL,
                production_type TEXT NOT NULL,
                adult_feed_grams REAL NOT NULL,
                adult_water_ml REAL NOT NULL,
                weekly_rates_json TEXT NOT NULL,
                notes TEXT
            );
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS egg_productions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                flock_id INTEGER NOT NULL,
                flock_name TEXT NOT NULL,
                fertile_count INTEGER NOT NULL DEFAULT 0,
                table_egg_count INTEGER NOT NULL DEFAULT 0,
                healthy_count INTEGER NOT NULL DEFAULT 0,
                broken_count INTEGER NOT NULL DEFAULT 0,
                dirty_count INTEGER NOT NULL DEFAULT 0,
                double_yolk_count INTEGER NOT NULL DEFAULT 0,
                total_count INTEGER NOT NULL,
                tray_count REAL NOT NULL DEFAULT 0.0,
                tray_gross_weight_grams REAL,
                net_egg_average_weight_grams REAL,
                laying_rate_percent REAL NOT NULL DEFAULT 0.0,
                date_jalali_str TEXT NOT NULL,
                timestamp_millis INTEGER NOT NULL,
                notes TEXT,
                FOREIGN KEY(flock_id) REFERENCES flocks(id) ON DELETE CASCADE
            );
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS flock_mortality_logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                flock_id INTEGER NOT NULL,
                flock_name TEXT NOT NULL,
                count INTEGER NOT NULL,
                reason TEXT NOT NULL,
                diagnosis_hint TEXT,
                date_jalali_str TEXT NOT NULL,
                timestamp_millis INTEGER NOT NULL,
                notes TEXT,
                FOREIGN KEY(flock_id) REFERENCES flocks(id) ON DELETE CASCADE
            );
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS flock_bird_sales (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                flock_id INTEGER NOT NULL,
                flock_name TEXT NOT NULL,
                bird_count INTEGER NOT NULL,
                bird_age_days INTEGER NOT NULL DEFAULT 1,
                live_weight_kg REAL,
                unit_price_tomans REAL,
                total_revenue_tomans REAL NOT NULL DEFAULT 0.0,
                buyer_name TEXT,
                linked_transaction_id INTEGER,
                date_jalali_str TEXT NOT NULL,
                timestamp_millis INTEGER NOT NULL,
                notes TEXT,
                FOREIGN KEY(flock_id) REFERENCES flocks(id) ON DELETE CASCADE
            );
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS flock_bird_slaughters (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                flock_id INTEGER NOT NULL,
                flock_name TEXT NOT NULL,
                bird_count INTEGER NOT NULL,
                bird_age_days INTEGER NOT NULL DEFAULT 1,
                live_weight_kg REAL,
                carcass_weight_kg REAL,
                date_jalali_str TEXT NOT NULL,
                timestamp_millis INTEGER NOT NULL,
                notes TEXT,
                FOREIGN KEY(flock_id) REFERENCES flocks(id) ON DELETE CASCADE
            );
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS farm_contacts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                full_name TEXT NOT NULL,
                business_name TEXT,
                category TEXT NOT NULL,
                phone_primary TEXT NOT NULL,
                phone_secondary TEXT,
                address TEXT,
                province_city TEXT,
                balance_tomans REAL NOT NULL DEFAULT 0.0,
                payment_terms_notes TEXT,
                rating_stars INTEGER NOT NULL DEFAULT 5,
                created_at_millis INTEGER NOT NULL
            );
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS financial_transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                amount_tomans REAL NOT NULL,
                type TEXT NOT NULL,
                category TEXT NOT NULL,
                date_jalali_str TEXT NOT NULL,
                timestamp_millis INTEGER NOT NULL,
                related_flock_id INTEGER,
                related_flock_name TEXT,
                related_warehouse_item_id INTEGER,
                quantity REAL NOT NULL DEFAULT 0.0,
                unit TEXT NOT NULL DEFAULT '',
                unit_price_tomans REAL NOT NULL DEFAULT 0.0,
                notes TEXT
            );
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS bird_transfers (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                from_flock_id INTEGER NOT NULL,
                from_flock_name TEXT NOT NULL,
                to_flock_id INTEGER NOT NULL,
                to_flock_name TEXT NOT NULL,
                bird_count INTEGER NOT NULL,
                date_jalali_str TEXT NOT NULL,
                timestamp_millis INTEGER NOT NULL,
                notes TEXT
            );
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS ration_plans (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                item_type TEXT NOT NULL,
                target_bird TEXT NOT NULL,
                target_stage TEXT NOT NULL,
                pricing_mode TEXT NOT NULL,
                manual_price_per_kg REAL NOT NULL DEFAULT 0.0,
                batch_base_kg REAL NOT NULL DEFAULT 100.0,
                ingredients_json TEXT NOT NULL,
                notes TEXT,
                created_at_millis INTEGER NOT NULL,
                updated_at_millis INTEGER NOT NULL
            );
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS custom_feed_ingredients (
                id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                category TEXT NOT NULL,
                energy_kcal_kg REAL,
                crude_protein REAL,
                calcium REAL,
                available_phosphorus REAL,
                methionine REAL,
                lysine REAL,
                sodium REAL,
                crude_fiber REAL,
                fat REAL,
                price_per_kg REAL,
                description TEXT,
                created_at_millis INTEGER
            );
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS flock_observations (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                flock_id INTEGER NOT NULL,
                flock_name TEXT NOT NULL,
                note TEXT NOT NULL,
                author_role TEXT NOT NULL DEFAULT 'MANAGER',
                category TEXT NOT NULL DEFAULT 'OBSERVATION',
                date_jalali_str TEXT NOT NULL,
                timestamp_millis INTEGER NOT NULL,
                FOREIGN KEY(flock_id) REFERENCES flocks(id) ON DELETE CASCADE
            );
        """.trimIndent())
        ensureDefaultRationPlans(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE calendar_schedules ADD COLUMN schedule_mode TEXT NOT NULL DEFAULT 'WEEKLY';")
                db.execSQL("ALTER TABLE calendar_schedules ADD COLUMN day_of_month INTEGER;")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (oldVersion < 3) {
            try {
                db.execSQL("ALTER TABLE flocks ADD COLUMN subgroups_json TEXT;")
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                db.execSQL("ALTER TABLE flocks ADD COLUMN is_feed_enabled INTEGER NOT NULL DEFAULT 1;")
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                db.execSQL("ALTER TABLE flocks ADD COLUMN is_water_enabled INTEGER NOT NULL DEFAULT 1;")
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS warehouse_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        category TEXT NOT NULL,
                        current_stock REAL NOT NULL,
                        unit TEXT NOT NULL,
                        assigned_flock_ids_csv TEXT,
                        min_alert_threshold REAL NOT NULL DEFAULT 50.0,
                        notes TEXT,
                        updated_at_millis INTEGER NOT NULL
                    );
                """.trimIndent())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (oldVersion < 4) {
            try {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS custom_breeds (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        species TEXT NOT NULL,
                        production_type TEXT NOT NULL,
                        adult_feed_grams REAL NOT NULL,
                        adult_water_ml REAL NOT NULL,
                        weekly_rates_json TEXT NOT NULL,
                        notes TEXT
                    );
                """.trimIndent())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (oldVersion < 5) {
            try {
                db.execSQL("ALTER TABLE warehouse_items ADD COLUMN price_per_unit REAL NOT NULL DEFAULT 0.0;")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (oldVersion < 6) {
            try {
                // Safeguard: Only clear legacy demo entries if they match exact default initial timestamps and demo flags
                db.execSQL("DELETE FROM flocks WHERE (name = 'سالن ۱ - گوشتی راس' OR name = 'سالن ۲ - تخمگذار هایلاین') AND created_at_millis <= 1700000000000L;")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (oldVersion < 7) {
            try {
                db.execSQL("UPDATE app_settings SET value = '' WHERE key = 'farm_city' AND value = 'شیراز';")
                db.execSQL("UPDATE app_settings SET value = '' WHERE key = 'farm_province' AND value = 'فارس';")
                db.execSQL("UPDATE app_settings SET value = '' WHERE key = 'farm_latitude' AND value = '29.59';")
                db.execSQL("UPDATE app_settings SET value = '' WHERE key = 'farm_longitude' AND value = '52.58';")
                db.execSQL("DELETE FROM app_settings WHERE key = 'manual_temp_celsius' AND value = '24.0';")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (oldVersion < 8) {
            try {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS egg_productions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        flock_id INTEGER NOT NULL,
                        flock_name TEXT NOT NULL,
                        healthy_count INTEGER NOT NULL,
                        broken_count INTEGER NOT NULL DEFAULT 0,
                        dirty_count INTEGER NOT NULL DEFAULT 0,
                        double_yolk_count INTEGER NOT NULL DEFAULT 0,
                        total_count INTEGER NOT NULL,
                        laying_rate_percent REAL NOT NULL DEFAULT 0.0,
                        date_jalali_str TEXT NOT NULL,
                        timestamp_millis INTEGER NOT NULL,
                        notes TEXT,
                        FOREIGN KEY(flock_id) REFERENCES flocks(id) ON DELETE CASCADE
                    );
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS financial_transactions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        title TEXT NOT NULL,
                        amount_tomans REAL NOT NULL,
                        type TEXT NOT NULL,
                        category TEXT NOT NULL,
                        date_jalali_str TEXT NOT NULL,
                        timestamp_millis INTEGER NOT NULL,
                        related_flock_id INTEGER,
                        related_flock_name TEXT,
                        related_warehouse_item_id INTEGER,
                        quantity REAL NOT NULL DEFAULT 0.0,
                        unit TEXT NOT NULL DEFAULT '',
                        unit_price_tomans REAL NOT NULL DEFAULT 0.0,
                        notes TEXT
                    );
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS bird_transfers (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        from_flock_id INTEGER NOT NULL,
                        from_flock_name TEXT NOT NULL,
                        to_flock_id INTEGER NOT NULL,
                        to_flock_name TEXT NOT NULL,
                        bird_count INTEGER NOT NULL,
                        date_jalali_str TEXT NOT NULL,
                        timestamp_millis INTEGER NOT NULL,
                        notes TEXT
                    );
                """.trimIndent())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (oldVersion < 9) {
            try {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS ration_plans (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        name TEXT NOT NULL,
                        item_type TEXT NOT NULL,
                        target_bird TEXT NOT NULL,
                        target_stage TEXT NOT NULL,
                        pricing_mode TEXT NOT NULL,
                        manual_price_per_kg REAL NOT NULL DEFAULT 0.0,
                        batch_base_kg REAL NOT NULL DEFAULT 100.0,
                        ingredients_json TEXT NOT NULL,
                        notes TEXT,
                        created_at_millis INTEGER NOT NULL,
                        updated_at_millis INTEGER NOT NULL
                    );
                """.trimIndent())
                ensureDefaultRationPlans(db)

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS custom_feed_ingredients (
                    id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    category TEXT NOT NULL,
                    energy_kcal_kg REAL,
                    crude_protein REAL,
                    calcium REAL,
                    available_phosphorus REAL,
                    methionine REAL,
                    lysine REAL,
                    sodium REAL,
                    crude_fiber REAL,
                    fat REAL,
                    price_per_kg REAL,
                    description TEXT,
                    created_at_millis INTEGER
                );
            """.trimIndent())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (oldVersion < 10) {
            try {
                db.execSQL("ALTER TABLE warehouse_items ADD COLUMN is_for_sale INTEGER NOT NULL DEFAULT 0;")
                db.execSQL("UPDATE warehouse_items SET is_for_sale = 1 WHERE category = 'EGG';")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (oldVersion < 11) {
            try {
                db.execSQL("ALTER TABLE egg_productions ADD COLUMN fertile_count INTEGER NOT NULL DEFAULT 0;")
                db.execSQL("ALTER TABLE egg_productions ADD COLUMN table_egg_count INTEGER NOT NULL DEFAULT 0;")
                db.execSQL("ALTER TABLE egg_productions ADD COLUMN tray_count REAL NOT NULL DEFAULT 0.0;")
                db.execSQL("ALTER TABLE egg_productions ADD COLUMN tray_gross_weight_grams REAL;")
                db.execSQL("ALTER TABLE egg_productions ADD COLUMN net_egg_average_weight_grams REAL;")
                db.execSQL("UPDATE egg_productions SET table_egg_count = healthy_count WHERE table_egg_count = 0;")
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS flock_mortality_logs (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        flock_id INTEGER NOT NULL,
                        flock_name TEXT NOT NULL,
                        count INTEGER NOT NULL,
                        reason TEXT NOT NULL,
                        diagnosis_hint TEXT,
                        date_jalali_str TEXT NOT NULL,
                        timestamp_millis INTEGER NOT NULL,
                        notes TEXT,
                        FOREIGN KEY(flock_id) REFERENCES flocks(id) ON DELETE CASCADE
                    );
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS farm_contacts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        full_name TEXT NOT NULL,
                        business_name TEXT,
                        category TEXT NOT NULL,
                        phone_primary TEXT NOT NULL,
                        phone_secondary TEXT,
                        address TEXT,
                        province_city TEXT,
                        balance_tomans REAL NOT NULL DEFAULT 0.0,
                        payment_terms_notes TEXT,
                        rating_stars INTEGER NOT NULL DEFAULT 5,
                        created_at_millis INTEGER NOT NULL
                    );
                """.trimIndent())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (oldVersion < 13) {
            try {
                ensureBirdExitTablesAndMigrateLegacySales(db)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun ensureBirdExitTablesAndMigrateLegacySales(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS flock_bird_sales (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                flock_id INTEGER NOT NULL,
                flock_name TEXT NOT NULL,
                bird_count INTEGER NOT NULL,
                bird_age_days INTEGER NOT NULL DEFAULT 1,
                live_weight_kg REAL,
                unit_price_tomans REAL,
                total_revenue_tomans REAL NOT NULL DEFAULT 0.0,
                buyer_name TEXT,
                linked_transaction_id INTEGER,
                date_jalali_str TEXT NOT NULL,
                timestamp_millis INTEGER NOT NULL,
                notes TEXT,
                FOREIGN KEY(flock_id) REFERENCES flocks(id) ON DELETE CASCADE
            );
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS flock_bird_slaughters (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                flock_id INTEGER NOT NULL,
                flock_name TEXT NOT NULL,
                bird_count INTEGER NOT NULL,
                bird_age_days INTEGER NOT NULL DEFAULT 1,
                live_weight_kg REAL,
                carcass_weight_kg REAL,
                date_jalali_str TEXT NOT NULL,
                timestamp_millis INTEGER NOT NULL,
                notes TEXT,
                FOREIGN KEY(flock_id) REFERENCES flocks(id) ON DELETE CASCADE
            );
        """.trimIndent())

        // Migrate any historical LIVE_SALE entries out of flock_mortality_logs without re-deducting birdCount
        val legacyCount = try {
            db.compileStatement("SELECT COUNT(*) FROM flock_mortality_logs WHERE reason = 'LIVE_SALE'").simpleQueryForLong()
        } catch (_: Exception) {
            0L
        }
        if (legacyCount > 0L) {
            db.execSQL("""
                INSERT INTO flock_bird_sales (
                    flock_id, flock_name, bird_count, bird_age_days, total_revenue_tomans, date_jalali_str, timestamp_millis, notes
                )
                SELECT
                    m.flock_id,
                    m.flock_name,
                    m.count,
                    COALESCE((SELECT MAX(1, CAST(((m.timestamp_millis - f.birth_epoch_millis) / 86400000) AS INTEGER) + 1) FROM flocks f WHERE f.id = m.flock_id), 1),
                    0.0,
                    m.date_jalali_str,
                    m.timestamp_millis,
                    COALESCE(NULLIF(TRIM(m.notes), ''), NULLIF(TRIM(m.diagnosis_hint), ''), 'منتقل‌شده از سوابق قدیمی فروش زنده')
                FROM flock_mortality_logs m
                WHERE m.reason = 'LIVE_SALE';
            """.trimIndent())
            db.execSQL("DELETE FROM flock_mortality_logs WHERE reason = 'LIVE_SALE';")
        }
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        try {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS custom_breeds (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    species TEXT NOT NULL,
                    production_type TEXT NOT NULL,
                    adult_feed_grams REAL NOT NULL,
                    adult_water_ml REAL NOT NULL,
                    weekly_rates_json TEXT NOT NULL,
                    notes TEXT
                );
            """.trimIndent())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            db.execSQL("ALTER TABLE warehouse_items ADD COLUMN price_per_unit REAL NOT NULL DEFAULT 0.0;")
        } catch (e: Exception) {
            // Already exists
        }
        try {
            db.execSQL("ALTER TABLE flocks ADD COLUMN custom_temp_celsius REAL;")
        } catch (e: Exception) {
            // Already exists
        }
        try {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS egg_productions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    flock_id INTEGER NOT NULL,
                    flock_name TEXT NOT NULL,
                    healthy_count INTEGER NOT NULL,
                    broken_count INTEGER NOT NULL DEFAULT 0,
                    dirty_count INTEGER NOT NULL DEFAULT 0,
                    double_yolk_count INTEGER NOT NULL DEFAULT 0,
                    total_count INTEGER NOT NULL,
                    laying_rate_percent REAL NOT NULL DEFAULT 0.0,
                    date_jalali_str TEXT NOT NULL,
                    timestamp_millis INTEGER NOT NULL,
                    notes TEXT,
                    FOREIGN KEY(flock_id) REFERENCES flocks(id) ON DELETE CASCADE
                );
            """.trimIndent())
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS financial_transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    amount_tomans REAL NOT NULL,
                    type TEXT NOT NULL,
                    category TEXT NOT NULL,
                    date_jalali_str TEXT NOT NULL,
                    timestamp_millis INTEGER NOT NULL,
                    related_flock_id INTEGER,
                    related_flock_name TEXT,
                    related_warehouse_item_id INTEGER,
                    quantity REAL NOT NULL DEFAULT 0.0,
                    unit TEXT NOT NULL DEFAULT '',
                    unit_price_tomans REAL NOT NULL DEFAULT 0.0,
                    notes TEXT
                );
            """.trimIndent())
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS bird_transfers (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    from_flock_id INTEGER NOT NULL,
                    from_flock_name TEXT NOT NULL,
                    to_flock_id INTEGER NOT NULL,
                    to_flock_name TEXT NOT NULL,
                    bird_count INTEGER NOT NULL,
                    date_jalali_str TEXT NOT NULL,
                    timestamp_millis INTEGER NOT NULL,
                    notes TEXT
                );
            """.trimIndent())
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS ration_plans (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    item_type TEXT NOT NULL,
                    target_bird TEXT NOT NULL,
                    target_stage TEXT NOT NULL,
                    pricing_mode TEXT NOT NULL,
                    manual_price_per_kg REAL NOT NULL DEFAULT 0.0,
                    batch_base_kg REAL NOT NULL DEFAULT 100.0,
                    ingredients_json TEXT NOT NULL,
                    notes TEXT,
                    created_at_millis INTEGER NOT NULL,
                    updated_at_millis INTEGER NOT NULL
                );
            """.trimIndent())
            ensureDefaultRationPlans(db)

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS custom_feed_ingredients (
                    id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    category TEXT NOT NULL,
                    energy_kcal_kg REAL,
                    crude_protein REAL,
                    calcium REAL,
                    available_phosphorus REAL,
                    methionine REAL,
                    lysine REAL,
                    sodium REAL,
                    crude_fiber REAL,
                    fat REAL,
                    price_per_kg REAL,
                    description TEXT,
                    created_at_millis INTEGER
                );
            """.trimIndent())

            try {
                db.execSQL("ALTER TABLE egg_productions ADD COLUMN fertile_count INTEGER NOT NULL DEFAULT 0;")
            } catch (ignored: Exception) {}
            try {
                db.execSQL("ALTER TABLE egg_productions ADD COLUMN table_egg_count INTEGER NOT NULL DEFAULT 0;")
            } catch (ignored: Exception) {}
            try {
                db.execSQL("ALTER TABLE egg_productions ADD COLUMN tray_count REAL NOT NULL DEFAULT 0.0;")
            } catch (ignored: Exception) {}
            try {
                db.execSQL("ALTER TABLE egg_productions ADD COLUMN tray_gross_weight_grams REAL;")
            } catch (ignored: Exception) {}
            try {
                db.execSQL("ALTER TABLE egg_productions ADD COLUMN net_egg_average_weight_grams REAL;")
            } catch (ignored: Exception) {}

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS flock_mortality_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    flock_id INTEGER NOT NULL,
                    flock_name TEXT NOT NULL,
                    count INTEGER NOT NULL,
                    reason TEXT NOT NULL,
                    diagnosis_hint TEXT,
                    date_jalali_str TEXT NOT NULL,
                    timestamp_millis INTEGER NOT NULL,
                    notes TEXT,
                    FOREIGN KEY(flock_id) REFERENCES flocks(id) ON DELETE CASCADE
                );
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS farm_contacts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    full_name TEXT NOT NULL,
                    business_name TEXT,
                    category TEXT NOT NULL,
                    phone_primary TEXT NOT NULL,
                    phone_secondary TEXT,
                    address TEXT,
                    province_city TEXT,
                    balance_tomans REAL NOT NULL DEFAULT 0.0,
                    payment_terms_notes TEXT,
                    rating_stars INTEGER NOT NULL DEFAULT 5,
                    created_at_millis INTEGER NOT NULL
                );
            """.trimIndent())

            try {
                db.execSQL("ALTER TABLE flocks ADD COLUMN medication_withdrawal_end_millis INTEGER;")
            } catch (ignored: Exception) {}
            try {
                db.execSQL("ALTER TABLE flocks ADD COLUMN active_medication_name TEXT;")
            } catch (ignored: Exception) {}

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS biosecurity_checks (
                    id TEXT PRIMARY KEY,
                    season TEXT NOT NULL,
                    title TEXT NOT NULL,
                    description TEXT NOT NULL,
                    priority TEXT NOT NULL DEFAULT 'HIGH',
                    is_completed INTEGER NOT NULL DEFAULT 0,
                    completed_date_jalali_str TEXT
                );
            """.trimIndent())
            ensureDefaultBiosecurityChecks(db)

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS flock_observations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    flock_id INTEGER NOT NULL,
                    flock_name TEXT NOT NULL,
                    note TEXT NOT NULL,
                    author_role TEXT NOT NULL DEFAULT 'MANAGER',
                    category TEXT NOT NULL DEFAULT 'OBSERVATION',
                    date_jalali_str TEXT NOT NULL,
                    timestamp_millis INTEGER NOT NULL,
                    FOREIGN KEY(flock_id) REFERENCES flocks(id) ON DELETE CASCADE
                );
            """.trimIndent())
            ensureBirdExitTablesAndMigrateLegacySales(db)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ==================== FLOCK SUBGROUPS HELPER ====================

    private fun serializeSubGroups(subGroups: List<FlockSubGroup>): String {
        val arr = JSONArray()
        for (sg in subGroups) {
            val obj = JSONObject().apply {
                put("id", sg.id)
                put("species", sg.species.name)
                put("breed", sg.breed)
                put("birdCount", sg.birdCount)
                put("birthEpochMillis", sg.birthEpochMillis)
                put("ageDays", sg.ageDays)
                if (sg.notes != null) put("notes", sg.notes)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun parseSubGroups(
        jsonStr: String?,
        flockCreatedAtMillis: Long = System.currentTimeMillis(),
        flockBirthMillis: Long = 0L
    ): List<FlockSubGroup> {
        if (jsonStr.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<FlockSubGroup>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val savedAgeDays = obj.optInt("ageDays", 1)
                val birthMillis = if (obj.has("birthEpochMillis") && !obj.isNull("birthEpochMillis")) {
                    obj.getLong("birthEpochMillis")
                } else if (flockCreatedAtMillis > 0L) {
                    flockCreatedAtMillis - (savedAgeDays.coerceAtLeast(1) * 24L * 60 * 60 * 1000)
                } else if (flockBirthMillis > 0L) {
                    flockBirthMillis
                } else {
                    System.currentTimeMillis() - (savedAgeDays.coerceAtLeast(1) * 24L * 60 * 60 * 1000)
                }

                list.add(
                    FlockSubGroup(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        species = BirdSpecies.fromString(obj.getString("species")),
                        breed = obj.getString("breed"),
                        birdCount = obj.getInt("birdCount"),
                        birthEpochMillis = birthMillis,
                        notes = if (obj.has("notes") && !obj.isNull("notes")) obj.getString("notes") else null
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ==================== FLOCKS ====================

    fun insertFlock(flock: Flock): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("name", flock.name)
            put("species", flock.species.name)
            put("breed", flock.breed)
            put("birth_epoch_millis", flock.birthEpochMillis)
            put("bird_count", flock.effectiveBirdCount)
            put("photo_uri", flock.photoUri)
            put("notes", flock.notes)
            put("created_at_millis", flock.createdAtMillis)
            put("subgroups_json", serializeSubGroups(flock.subGroups))
            put("is_feed_enabled", if (flock.isFeedEnabled) 1 else 0)
            put("is_water_enabled", if (flock.isWaterEnabled) 1 else 0)
            if (flock.customTempCelsius != null) {
                put("custom_temp_celsius", flock.customTempCelsius)
            } else {
                putNull("custom_temp_celsius")
            }
            if (flock.medicationWithdrawalEndMillis != null) {
                put("medication_withdrawal_end_millis", flock.medicationWithdrawalEndMillis)
            } else {
                putNull("medication_withdrawal_end_millis")
            }
            if (flock.activeMedicationName != null) {
                put("active_medication_name", flock.activeMedicationName)
            } else {
                putNull("active_medication_name")
            }
        }
        val id = db.insert("flocks", null, cv)
        notifyDataChanged()
        return id
    }

    fun updateFlock(flock: Flock): Boolean {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("name", flock.name)
            put("species", flock.species.name)
            put("breed", flock.breed)
            put("birth_epoch_millis", flock.birthEpochMillis)
            put("bird_count", flock.effectiveBirdCount)
            put("photo_uri", flock.photoUri)
            put("notes", flock.notes)
            put("subgroups_json", serializeSubGroups(flock.subGroups))
            put("is_feed_enabled", if (flock.isFeedEnabled) 1 else 0)
            put("is_water_enabled", if (flock.isWaterEnabled) 1 else 0)
            if (flock.customTempCelsius != null) {
                put("custom_temp_celsius", flock.customTempCelsius)
            } else {
                putNull("custom_temp_celsius")
            }
            if (flock.medicationWithdrawalEndMillis != null) {
                put("medication_withdrawal_end_millis", flock.medicationWithdrawalEndMillis)
            } else {
                putNull("medication_withdrawal_end_millis")
            }
            if (flock.activeMedicationName != null) {
                put("active_medication_name", flock.activeMedicationName)
            } else {
                putNull("active_medication_name")
            }
        }
        val rows = db.update("flocks", cv, "id = ?", arrayOf(flock.id.toString()))
        notifyDataChanged()
        return rows > 0
    }

    fun deleteFlock(flockId: Long): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            val fIdStr = arrayOf(flockId.toString())
            db.delete("feed_water_charges", "flock_id = ?", fIdStr)
            db.delete("calendar_schedules", "flock_id = ?", fIdStr)
            db.delete("flock_mortality_logs", "flock_id = ?", fIdStr)
            db.delete("flock_bird_sales", "flock_id = ?", fIdStr)
            db.delete("flock_bird_slaughters", "flock_id = ?", fIdStr)
            db.delete("flock_observations", "flock_id = ?", fIdStr)
            db.delete("egg_productions", "flock_id = ?", fIdStr)
            db.delete("bird_transfers", "from_flock_id = ? OR to_flock_id = ?", arrayOf(flockId.toString(), flockId.toString()))
            val rows = db.delete("flocks", "id = ?", fIdStr)
            db.setTransactionSuccessful()
            notifyDataChanged()
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            db.endTransaction()
        }
    }

    fun getAllFlocks(): List<Flock> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM flocks ORDER BY id DESC", null)
        val list = mutableListOf<Flock>()
        cursor.use {
            while (it.moveToNext()) {
                val createdAt = it.getLong(it.getColumnIndexOrThrow("created_at_millis"))
                val birthEpoch = it.getLong(it.getColumnIndexOrThrow("birth_epoch_millis"))
                val sgIdx = it.getColumnIndex("subgroups_json")
                val subGroups = if (sgIdx != -1 && !it.isNull(sgIdx)) parseSubGroups(it.getString(sgIdx), createdAt, birthEpoch) else emptyList()
                val feedEnIdx = it.getColumnIndex("is_feed_enabled")
                val isFeedEnabled = if (feedEnIdx != -1 && !it.isNull(feedEnIdx)) it.getInt(feedEnIdx) == 1 else true
                val waterEnIdx = it.getColumnIndex("is_water_enabled")
                val isWaterEnabled = if (waterEnIdx != -1 && !it.isNull(waterEnIdx)) it.getInt(waterEnIdx) == 1 else true
                val tempIdx = it.getColumnIndex("custom_temp_celsius")
                val customTempCelsius = if (tempIdx != -1 && !it.isNull(tempIdx)) it.getDouble(tempIdx) else null
                val medEndIdx = it.getColumnIndex("medication_withdrawal_end_millis")
                val medEndMillis = if (medEndIdx != -1 && !it.isNull(medEndIdx)) it.getLong(medEndIdx) else null
                val medNameIdx = it.getColumnIndex("active_medication_name")
                val medName = if (medNameIdx != -1 && !it.isNull(medNameIdx)) it.getString(medNameIdx) else null

                list.add(
                    Flock(
                        id = it.getLong(it.getColumnIndexOrThrow("id")),
                        name = it.getString(it.getColumnIndexOrThrow("name")),
                        species = BirdSpecies.fromString(it.getString(it.getColumnIndexOrThrow("species"))),
                        breed = it.getString(it.getColumnIndexOrThrow("breed")),
                        birthEpochMillis = birthEpoch,
                        birdCount = it.getInt(it.getColumnIndexOrThrow("bird_count")),
                        photoUri = it.getString(it.getColumnIndexOrThrow("photo_uri")),
                        notes = it.getString(it.getColumnIndexOrThrow("notes")),
                        createdAtMillis = createdAt,
                        subGroups = subGroups,
                        isFeedEnabled = isFeedEnabled,
                        isWaterEnabled = isWaterEnabled,
                        customTempCelsius = customTempCelsius,
                        medicationWithdrawalEndMillis = medEndMillis,
                        activeMedicationName = medName
                    )
                )
            }
        }
        return list
    }

    fun getFlockById(id: Long): Flock? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM flocks WHERE id = ?", arrayOf(id.toString()))
        cursor.use {
            if (it.moveToFirst()) {
                val createdAt = it.getLong(it.getColumnIndexOrThrow("created_at_millis"))
                val birthEpoch = it.getLong(it.getColumnIndexOrThrow("birth_epoch_millis"))
                val sgIdx = it.getColumnIndex("subgroups_json")
                val subGroups = if (sgIdx != -1 && !it.isNull(sgIdx)) parseSubGroups(it.getString(sgIdx), createdAt, birthEpoch) else emptyList()
                val feedEnIdx = it.getColumnIndex("is_feed_enabled")
                val isFeedEnabled = if (feedEnIdx != -1 && !it.isNull(feedEnIdx)) it.getInt(feedEnIdx) == 1 else true
                val waterEnIdx = it.getColumnIndex("is_water_enabled")
                val isWaterEnabled = if (waterEnIdx != -1 && !it.isNull(waterEnIdx)) it.getInt(waterEnIdx) == 1 else true
                val tempIdx = it.getColumnIndex("custom_temp_celsius")
                val customTempCelsius = if (tempIdx != -1 && !it.isNull(tempIdx)) it.getDouble(tempIdx) else null
                val medEndIdx = it.getColumnIndex("medication_withdrawal_end_millis")
                val medEndMillis = if (medEndIdx != -1 && !it.isNull(medEndIdx)) it.getLong(medEndIdx) else null
                val medNameIdx = it.getColumnIndex("active_medication_name")
                val medName = if (medNameIdx != -1 && !it.isNull(medNameIdx)) it.getString(medNameIdx) else null

                return Flock(
                    id = it.getLong(it.getColumnIndexOrThrow("id")),
                    name = it.getString(it.getColumnIndexOrThrow("name")),
                    species = BirdSpecies.fromString(it.getString(it.getColumnIndexOrThrow("species"))),
                    breed = it.getString(it.getColumnIndexOrThrow("breed")),
                    birthEpochMillis = birthEpoch,
                    birdCount = it.getInt(it.getColumnIndexOrThrow("bird_count")),
                    photoUri = it.getString(it.getColumnIndexOrThrow("photo_uri")),
                    notes = it.getString(it.getColumnIndexOrThrow("notes")),
                    createdAtMillis = createdAt,
                    subGroups = subGroups,
                    isFeedEnabled = isFeedEnabled,
                    isWaterEnabled = isWaterEnabled,
                    customTempCelsius = customTempCelsius,
                    medicationWithdrawalEndMillis = medEndMillis,
                    activeMedicationName = medName
                )
            }
        }
        return null
    }

    // ==================== FLOCK OBSERVATION NOTES ====================

    fun insertFlockObservation(obs: FlockObservationNote): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("flock_id", obs.flockId)
            put("flock_name", obs.flockName)
            put("note", obs.note)
            put("author_role", obs.authorRole.name)
            put("category", obs.category)
            put("date_jalali_str", obs.dateJalaliStr)
            put("timestamp_millis", obs.timestampMillis)
        }
        val id = db.insert("flock_observations", null, cv)
        notifyDataChanged()
        return id
    }

    fun getFlockObservations(flockId: Long? = null): List<FlockObservationNote> {
        val db = readableDatabase
        val query = if (flockId != null) {
            "SELECT * FROM flock_observations WHERE flock_id = ? ORDER BY timestamp_millis DESC"
        } else {
            "SELECT * FROM flock_observations ORDER BY timestamp_millis DESC"
        }
        val args = if (flockId != null) arrayOf(flockId.toString()) else null
        val cursor = db.rawQuery(query, args)
        val list = mutableListOf<FlockObservationNote>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    FlockObservationNote(
                        id = it.getLong(it.getColumnIndexOrThrow("id")),
                        flockId = it.getLong(it.getColumnIndexOrThrow("flock_id")),
                        flockName = it.getString(it.getColumnIndexOrThrow("flock_name")),
                        note = it.getString(it.getColumnIndexOrThrow("note")),
                        authorRole = try {
                            UserRole.valueOf(it.getString(it.getColumnIndexOrThrow("author_role")))
                        } catch (_: Exception) {
                            UserRole.MANAGER
                        },
                        category = it.getString(it.getColumnIndexOrThrow("category")),
                        dateJalaliStr = it.getString(it.getColumnIndexOrThrow("date_jalali_str")),
                        timestampMillis = it.getLong(it.getColumnIndexOrThrow("timestamp_millis"))
                    )
                )
            }
        }
        return list
    }

    fun deleteFlockObservation(id: Long): Boolean {
        val db = writableDatabase
        val rows = db.delete("flock_observations", "id = ?", arrayOf(id.toString()))
        if (rows > 0) notifyDataChanged()
        return rows > 0
    }

    // ==================== FEED & WATER CHARGES ====================

    fun insertCharge(charge: FeedWaterCharge): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("flock_id", charge.flockId)
            put("item_type", charge.itemType.name)
            put("charged_amount", charge.chargedAmount)
            put("charge_timestamp_millis", charge.chargeTimestampMillis)
            put("date_jalali_str", charge.dateJalaliStr)
            put("notes", charge.notes)
        }
        val id = db.insert("feed_water_charges", null, cv)
        notifyDataChanged()
        return id
    }

    fun getLatestChargeForFlock(flockId: Long, itemType: ChargeItemType): FeedWaterCharge? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM feed_water_charges WHERE flock_id = ? AND item_type = ? ORDER BY charge_timestamp_millis DESC LIMIT 1",
            arrayOf(flockId.toString(), itemType.name)
        )
        cursor.use {
            if (it.moveToFirst()) {
                return FeedWaterCharge(
                    id = it.getLong(it.getColumnIndexOrThrow("id")),
                    flockId = it.getLong(it.getColumnIndexOrThrow("flock_id")),
                    itemType = ChargeItemType.valueOf(it.getString(it.getColumnIndexOrThrow("item_type"))),
                    chargedAmount = it.getDouble(it.getColumnIndexOrThrow("charged_amount")),
                    chargeTimestampMillis = it.getLong(it.getColumnIndexOrThrow("charge_timestamp_millis")),
                    dateJalaliStr = it.getString(it.getColumnIndexOrThrow("date_jalali_str")),
                    notes = it.getString(it.getColumnIndexOrThrow("notes"))
                )
            }
        }
        return null
    }

    fun getChargesForFlockAndDate(flockId: Long, dateJalaliStr: String): List<FeedWaterCharge> {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM feed_water_charges WHERE flock_id = ? AND date_jalali_str = ? ORDER BY charge_timestamp_millis DESC",
            arrayOf(flockId.toString(), dateJalaliStr)
        )
        val list = mutableListOf<FeedWaterCharge>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    FeedWaterCharge(
                        id = it.getLong(it.getColumnIndexOrThrow("id")),
                        flockId = it.getLong(it.getColumnIndexOrThrow("flock_id")),
                        itemType = ChargeItemType.valueOf(it.getString(it.getColumnIndexOrThrow("item_type"))),
                        chargedAmount = it.getDouble(it.getColumnIndexOrThrow("charged_amount")),
                        chargeTimestampMillis = it.getLong(it.getColumnIndexOrThrow("charge_timestamp_millis")),
                        dateJalaliStr = it.getString(it.getColumnIndexOrThrow("date_jalali_str")),
                        notes = it.getString(it.getColumnIndexOrThrow("notes"))
                    )
                )
            }
        }
        return list
    }

    fun getAllChargesForFlock(flockId: Long, limit: Int = 30): List<FeedWaterCharge> {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM feed_water_charges WHERE flock_id = ? ORDER BY charge_timestamp_millis DESC LIMIT ?",
            arrayOf(flockId.toString(), limit.toString())
        )
        val list = mutableListOf<FeedWaterCharge>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    FeedWaterCharge(
                        id = it.getLong(it.getColumnIndexOrThrow("id")),
                        flockId = it.getLong(it.getColumnIndexOrThrow("flock_id")),
                        itemType = ChargeItemType.valueOf(it.getString(it.getColumnIndexOrThrow("item_type"))),
                        chargedAmount = it.getDouble(it.getColumnIndexOrThrow("charged_amount")),
                        chargeTimestampMillis = it.getLong(it.getColumnIndexOrThrow("charge_timestamp_millis")),
                        dateJalaliStr = it.getString(it.getColumnIndexOrThrow("date_jalali_str")),
                        notes = it.getString(it.getColumnIndexOrThrow("notes"))
                    )
                )
            }
        }
        return list
    }

    fun getAllCharges(limit: Int = 300): List<FeedWaterCharge> {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM feed_water_charges ORDER BY charge_timestamp_millis DESC LIMIT ?",
            arrayOf(limit.toString())
        )
        val list = mutableListOf<FeedWaterCharge>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    FeedWaterCharge(
                        id = it.getLong(it.getColumnIndexOrThrow("id")),
                        flockId = it.getLong(it.getColumnIndexOrThrow("flock_id")),
                        itemType = ChargeItemType.valueOf(it.getString(it.getColumnIndexOrThrow("item_type"))),
                        chargedAmount = it.getDouble(it.getColumnIndexOrThrow("charged_amount")),
                        chargeTimestampMillis = it.getLong(it.getColumnIndexOrThrow("charge_timestamp_millis")),
                        dateJalaliStr = it.getString(it.getColumnIndexOrThrow("date_jalali_str")),
                        notes = it.getString(it.getColumnIndexOrThrow("notes"))
                    )
                )
            }
        }
        return list
    }

    fun deleteCharge(id: Long): Boolean {
        val db = writableDatabase
        val rows = db.delete("feed_water_charges", "id = ?", arrayOf(id.toString()))
        notifyDataChanged()
        return rows > 0
    }

    // ==================== CALENDAR SCHEDULES ====================

    fun insertSchedule(schedule: CalendarSchedule): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("flock_id", schedule.flockId)
            put("flock_name", schedule.flockName)
            put("schedule_type", schedule.scheduleType)
            put("title", schedule.title)
            put("notes", schedule.notes)
            put("days_of_week_csv", schedule.daysOfWeek.joinToString(","))
            put("hour", schedule.hour)
            put("minute", schedule.minute)
            put("is_enabled", if (schedule.isEnabled) 1 else 0)
            put("schedule_mode", schedule.scheduleMode)
            put("day_of_month", schedule.dayOfMonth)
        }
        val id = db.insert("calendar_schedules", null, cv)
        notifyDataChanged()
        return id
    }

    fun updateSchedule(schedule: CalendarSchedule): Boolean {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("flock_id", schedule.flockId)
            put("flock_name", schedule.flockName)
            put("schedule_type", schedule.scheduleType)
            put("title", schedule.title)
            put("notes", schedule.notes)
            put("days_of_week_csv", schedule.daysOfWeek.joinToString(","))
            put("hour", schedule.hour)
            put("minute", schedule.minute)
            put("is_enabled", if (schedule.isEnabled) 1 else 0)
            put("schedule_mode", schedule.scheduleMode)
            put("day_of_month", schedule.dayOfMonth)
        }
        val rows = db.update("calendar_schedules", cv, "id = ?", arrayOf(schedule.id.toString()))
        notifyDataChanged()
        return rows > 0
    }

    fun deleteSchedule(scheduleId: Long): Boolean {
        val db = writableDatabase
        val rows = db.delete("calendar_schedules", "id = ?", arrayOf(scheduleId.toString()))
        notifyDataChanged()
        return rows > 0
    }

    fun toggleSchedule(scheduleId: Long, isEnabled: Boolean): Boolean {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("is_enabled", if (isEnabled) 1 else 0)
        }
        val rows = db.update("calendar_schedules", cv, "id = ?", arrayOf(scheduleId.toString()))
        notifyDataChanged()
        return rows > 0
    }

    fun getAllSchedules(): List<CalendarSchedule> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM calendar_schedules ORDER BY hour ASC, minute ASC", null)
        val list = mutableListOf<CalendarSchedule>()
        cursor.use {
            while (it.moveToNext()) {
                val daysCsv = it.getString(it.getColumnIndexOrThrow("days_of_week_csv"))
                val daysList = if (daysCsv.isNullOrBlank()) emptyList() else daysCsv.split(",").mapNotNull { d -> d.trim().toIntOrNull() }
                val modeIdx = it.getColumnIndex("schedule_mode")
                val dayOfMonthIdx = it.getColumnIndex("day_of_month")
                val mode = if (modeIdx != -1 && !it.isNull(modeIdx)) it.getString(modeIdx) else "WEEKLY"
                val dom = if (dayOfMonthIdx != -1 && !it.isNull(dayOfMonthIdx)) it.getInt(dayOfMonthIdx) else null

                list.add(
                    CalendarSchedule(
                        id = it.getLong(it.getColumnIndexOrThrow("id")),
                        flockId = if (it.isNull(it.getColumnIndexOrThrow("flock_id"))) null else it.getLong(it.getColumnIndexOrThrow("flock_id")),
                        flockName = it.getString(it.getColumnIndexOrThrow("flock_name")),
                        scheduleType = it.getString(it.getColumnIndexOrThrow("schedule_type")),
                        title = it.getString(it.getColumnIndexOrThrow("title")),
                        notes = it.getString(it.getColumnIndexOrThrow("notes")),
                        daysOfWeek = daysList,
                        hour = it.getInt(it.getColumnIndexOrThrow("hour")),
                        minute = it.getInt(it.getColumnIndexOrThrow("minute")),
                        isEnabled = it.getInt(it.getColumnIndexOrThrow("is_enabled")) == 1,
                        scheduleMode = mode,
                        dayOfMonth = dom
                    )
                )
            }
        }
        return list
    }

    // ==================== WAREHOUSE ITEMS ====================

    fun insertWarehouseItem(item: WarehouseItem): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("name", item.name)
            put("category", item.category.name)
            put("current_stock", item.currentStock)
            put("unit", item.unit)
            put("price_per_unit", item.pricePerUnit)
            put("assigned_flock_ids_csv", item.assignedFlockIds.joinToString(","))
            put("min_alert_threshold", item.minAlertThreshold)
            put("is_for_sale", if (item.isForSale) 1 else 0)
            put("notes", item.notes)
            put("updated_at_millis", item.updatedAtMillis)
        }
        val id = db.insert("warehouse_items", null, cv)
        notifyDataChanged()
        return id
    }

    fun updateWarehouseItem(item: WarehouseItem): Boolean {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("name", item.name)
            put("category", item.category.name)
            put("current_stock", item.currentStock)
            put("unit", item.unit)
            put("price_per_unit", item.pricePerUnit)
            put("assigned_flock_ids_csv", item.assignedFlockIds.joinToString(","))
            put("min_alert_threshold", item.minAlertThreshold)
            put("is_for_sale", if (item.isForSale) 1 else 0)
            put("notes", item.notes)
            put("updated_at_millis", System.currentTimeMillis())
        }
        val rows = db.update("warehouse_items", cv, "id = ?", arrayOf(item.id.toString()))
        notifyDataChanged()
        return rows > 0
    }

    fun deleteWarehouseItem(id: Long): Boolean {
        val db = writableDatabase
        val rows = db.delete("warehouse_items", "id = ?", arrayOf(id.toString()))
        notifyDataChanged()
        return rows > 0
    }

    fun getAllWarehouseItems(): List<WarehouseItem> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM warehouse_items ORDER BY id DESC", null)
        val list = mutableListOf<WarehouseItem>()
        cursor.use {
            while (it.moveToNext()) {
                val csv = it.getString(it.getColumnIndexOrThrow("assigned_flock_ids_csv"))
                val flockIds = if (csv.isNullOrBlank()) emptyList() else csv.split(",").mapNotNull { s -> s.trim().toLongOrNull() }
                val priceIdx = it.getColumnIndex("price_per_unit")
                val price = if (priceIdx != -1 && !it.isNull(priceIdx)) it.getDouble(priceIdx) else 0.0
                val saleIdx = it.getColumnIndex("is_for_sale")
                val isForSale = if (saleIdx != -1 && !it.isNull(saleIdx)) it.getInt(saleIdx) == 1 else false
                list.add(
                    WarehouseItem(
                        id = it.getLong(it.getColumnIndexOrThrow("id")),
                        name = it.getString(it.getColumnIndexOrThrow("name")),
                        category = WarehouseCategory.fromString(it.getString(it.getColumnIndexOrThrow("category"))),
                        currentStock = it.getDouble(it.getColumnIndexOrThrow("current_stock")),
                        unit = it.getString(it.getColumnIndexOrThrow("unit")),
                        pricePerUnit = price,
                        assignedFlockIds = flockIds,
                        minAlertThreshold = it.getDouble(it.getColumnIndexOrThrow("min_alert_threshold")),
                        isForSale = isForSale,
                        notes = it.getString(it.getColumnIndexOrThrow("notes")),
                        updatedAtMillis = it.getLong(it.getColumnIndexOrThrow("updated_at_millis"))
                    )
                )
            }
        }
        return list
    }

    fun getWarehouseItemById(id: Long): WarehouseItem? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM warehouse_items WHERE id = ?", arrayOf(id.toString()))
        cursor.use {
            if (it.moveToFirst()) {
                val csv = it.getString(it.getColumnIndexOrThrow("assigned_flock_ids_csv"))
                val flockIds = if (csv.isNullOrBlank()) emptyList() else csv.split(",").mapNotNull { s -> s.trim().toLongOrNull() }
                val priceIdx = it.getColumnIndex("price_per_unit")
                val price = if (priceIdx != -1 && !it.isNull(priceIdx)) it.getDouble(priceIdx) else 0.0
                val saleIdx = it.getColumnIndex("is_for_sale")
                val isForSale = if (saleIdx != -1 && !it.isNull(saleIdx)) it.getInt(saleIdx) == 1 else false
                return WarehouseItem(
                    id = it.getLong(it.getColumnIndexOrThrow("id")),
                    name = it.getString(it.getColumnIndexOrThrow("name")),
                    category = WarehouseCategory.fromString(it.getString(it.getColumnIndexOrThrow("category"))),
                    currentStock = it.getDouble(it.getColumnIndexOrThrow("current_stock")),
                    unit = it.getString(it.getColumnIndexOrThrow("unit")),
                    pricePerUnit = price,
                    assignedFlockIds = flockIds,
                    minAlertThreshold = it.getDouble(it.getColumnIndexOrThrow("min_alert_threshold")),
                    isForSale = isForSale,
                    notes = it.getString(it.getColumnIndexOrThrow("notes")),
                    updatedAtMillis = it.getLong(it.getColumnIndexOrThrow("updated_at_millis"))
                )
            }
        }
        return null
    }

    // ==================== BIRD TRANSFERS ====================

    fun transferBirds(fromFlockId: Long, toFlockId: Long, birdCount: Int, dateJalaliStr: String, notes: String?): Boolean {
        if (fromFlockId == toFlockId || birdCount <= 0) return false
        val fromFlock = getFlockById(fromFlockId) ?: return false
        val toFlock = getFlockById(toFlockId) ?: return false

        if (fromFlock.effectiveBirdCount < birdCount) return false

        // Reduce from fromFlock
        val updatedFromSubGroups = if (fromFlock.subGroups.isNotEmpty()) {
            var remainingToDeduct = birdCount
            fromFlock.subGroups.map { sg ->
                if (remainingToDeduct > 0) {
                    val deduct = minOf(sg.birdCount, remainingToDeduct)
                    remainingToDeduct -= deduct
                    sg.copy(birdCount = sg.birdCount - deduct)
                } else {
                    sg
                }
            }.filter { it.birdCount > 0 }
        } else emptyList()

        val newFromCount = (fromFlock.birdCount - birdCount).coerceAtLeast(0)
        updateFlock(fromFlock.copy(
            birdCount = if (updatedFromSubGroups.isNotEmpty()) updatedFromSubGroups.sumOf { it.birdCount } else newFromCount,
            subGroups = updatedFromSubGroups
        ))

        // Increase in toFlock
        val updatedToSubGroups = if (toFlock.subGroups.isNotEmpty()) {
            val firstSg = toFlock.subGroups.first()
            listOf(firstSg.copy(birdCount = firstSg.birdCount + birdCount)) + toFlock.subGroups.drop(1)
        } else emptyList()

        val newToCount = toFlock.birdCount + birdCount
        updateFlock(toFlock.copy(
            birdCount = if (updatedToSubGroups.isNotEmpty()) updatedToSubGroups.sumOf { it.birdCount } else newToCount,
            subGroups = updatedToSubGroups
        ))

        // Log the transfer
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("from_flock_id", fromFlockId)
            put("from_flock_name", fromFlock.name)
            put("to_flock_id", toFlockId)
            put("to_flock_name", toFlock.name)
            put("bird_count", birdCount)
            put("date_jalali_str", dateJalaliStr)
            put("timestamp_millis", System.currentTimeMillis())
            put("notes", notes)
        }
        db.insert("bird_transfers", null, cv)
        notifyDataChanged()
        return true
    }

    fun getAllBirdTransfers(): List<BirdTransfer> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM bird_transfers ORDER BY timestamp_millis DESC, id DESC", null)
        val list = mutableListOf<BirdTransfer>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    BirdTransfer(
                        id = it.getLong(it.getColumnIndexOrThrow("id")),
                        fromFlockId = it.getLong(it.getColumnIndexOrThrow("from_flock_id")),
                        fromFlockName = it.getString(it.getColumnIndexOrThrow("from_flock_name")),
                        toFlockId = it.getLong(it.getColumnIndexOrThrow("to_flock_id")),
                        toFlockName = it.getString(it.getColumnIndexOrThrow("to_flock_name")),
                        birdCount = it.getInt(it.getColumnIndexOrThrow("bird_count")),
                        dateJalaliStr = it.getString(it.getColumnIndexOrThrow("date_jalali_str")),
                        timestampMillis = it.getLong(it.getColumnIndexOrThrow("timestamp_millis")),
                        notes = it.getString(it.getColumnIndexOrThrow("notes"))
                    )
                )
            }
        }
        return list
    }

    // ==================== EGG PRODUCTIONS ====================

    fun insertEggProduction(production: EggProduction): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("flock_id", production.flockId)
            put("flock_name", production.flockName)
            put("fertile_count", production.fertileCount)
            put("table_egg_count", production.tableEggCount)
            put("healthy_count", production.healthyCount)
            put("broken_count", production.brokenCount)
            put("dirty_count", production.dirtyCount)
            put("double_yolk_count", production.doubleYolkCount)
            put("total_count", production.totalCount)
            put("tray_count", production.trayCount)
            if (production.trayGrossWeightGrams != null) put("tray_gross_weight_grams", production.trayGrossWeightGrams)
            if (production.netEggAverageWeightGrams != null) put("net_egg_average_weight_grams", production.netEggAverageWeightGrams)
            put("laying_rate_percent", production.layingRatePercent)
            put("date_jalali_str", production.dateJalaliStr)
            put("timestamp_millis", production.timestampMillis)
            put("notes", production.notes)
        }
        val id = db.insert("egg_productions", null, cv)

        // Automatically update warehouse inventory for fertile and table eggs if enabled in settings
        val isAutoAdd = getSetting("is_auto_add_eggs_warehouse", "true").toBoolean()
        if (isAutoAdd && id > 0) {
            val tableCount = if (production.tableEggCount > 0) production.tableEggCount else (if (production.fertileCount == 0) production.healthyCount else 0)
            autoAddProducedEggsToWarehouse(
                flockId = production.flockId,
                flockName = production.flockName,
                fertileCount = production.fertileCount,
                tableCount = tableCount,
                dateJalaliStr = production.dateJalaliStr
            )
        }

        notifyDataChanged()
        return id
    }

    fun getAllEggProductions(): List<EggProduction> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM egg_productions ORDER BY timestamp_millis DESC, id DESC", null)
        val list = mutableListOf<EggProduction>()
        cursor.use {
            while (it.moveToNext()) {
                val fertileIdx = it.getColumnIndex("fertile_count")
                val tableIdx = it.getColumnIndex("table_egg_count")
                val trayIdx = it.getColumnIndex("tray_count")
                val trayGrossIdx = it.getColumnIndex("tray_gross_weight_grams")
                val netAvgIdx = it.getColumnIndex("net_egg_average_weight_grams")

                val healthy = it.getInt(it.getColumnIndexOrThrow("healthy_count"))
                val fertile = if (fertileIdx >= 0 && !it.isNull(fertileIdx)) it.getInt(fertileIdx) else 0
                val tableEgg = if (tableIdx >= 0 && !it.isNull(tableIdx)) it.getInt(tableIdx) else (if (fertile == 0) healthy else 0)

                list.add(
                    EggProduction(
                        id = it.getLong(it.getColumnIndexOrThrow("id")),
                        flockId = it.getLong(it.getColumnIndexOrThrow("flock_id")),
                        flockName = it.getString(it.getColumnIndexOrThrow("flock_name")),
                        fertileCount = fertile,
                        tableEggCount = tableEgg,
                        healthyCount = healthy,
                        brokenCount = it.getInt(it.getColumnIndexOrThrow("broken_count")),
                        dirtyCount = it.getInt(it.getColumnIndexOrThrow("dirty_count")),
                        doubleYolkCount = it.getInt(it.getColumnIndexOrThrow("double_yolk_count")),
                        totalCount = it.getInt(it.getColumnIndexOrThrow("total_count")),
                        trayCount = if (trayIdx >= 0 && !it.isNull(trayIdx)) it.getDouble(trayIdx) else 0.0,
                        trayGrossWeightGrams = if (trayGrossIdx >= 0 && !it.isNull(trayGrossIdx)) it.getDouble(trayGrossIdx) else null,
                        netEggAverageWeightGrams = if (netAvgIdx >= 0 && !it.isNull(netAvgIdx)) it.getDouble(netAvgIdx) else null,
                        layingRatePercent = it.getDouble(it.getColumnIndexOrThrow("laying_rate_percent")),
                        dateJalaliStr = it.getString(it.getColumnIndexOrThrow("date_jalali_str")),
                        timestampMillis = it.getLong(it.getColumnIndexOrThrow("timestamp_millis")),
                        notes = it.getString(it.getColumnIndexOrThrow("notes"))
                    )
                )
            }
        }
        return list
    }

    // ==================== FLOCK MORTALITY LOGS ====================

    fun insertMortalityLog(log: FlockMortalityLog): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("flock_id", log.flockId)
            put("flock_name", log.flockName)
            put("count", log.count)
            put("reason", log.reason.name)
            put("diagnosis_hint", log.diagnosisHint)
            put("date_jalali_str", log.dateJalaliStr)
            put("timestamp_millis", log.timestampMillis)
            put("notes", log.notes)
        }
        val id = db.insert("flock_mortality_logs", null, cv)

        // Deduct birdCount from flock and its subGroups
        val flock = getFlockById(log.flockId)
        if (flock != null && log.count > 0) {
            val newBirdCount = (flock.birdCount - log.count).coerceAtLeast(0)
            val updatedSubGroups = if (flock.subGroups.isNotEmpty()) {
                var toDeduct = log.count
                val newSubList = flock.subGroups.toMutableList()
                // Deduct from subGroup with highest count first
                val sortedIndices = flock.subGroups.indices.sortedByDescending { flock.subGroups[it].birdCount }
                for (idx in sortedIndices) {
                    if (toDeduct <= 0) break
                    val currentCount = newSubList[idx].birdCount
                    val dec = currentCount.coerceAtMost(toDeduct)
                    newSubList[idx] = newSubList[idx].copy(birdCount = (currentCount - dec).coerceAtLeast(0))
                    toDeduct -= dec
                }
                newSubList
            } else {
                emptyList()
            }
            updateFlock(flock.copy(birdCount = newBirdCount, subGroups = updatedSubGroups))
        }

        notifyDataChanged()
        return id
    }

    fun getMortalityLogById(id: Long): FlockMortalityLog? {
        val db = readableDatabase
        var log: FlockMortalityLog? = null
        try {
            val cursor = db.rawQuery("SELECT * FROM flock_mortality_logs WHERE id = ? LIMIT 1", arrayOf(id.toString()))
            cursor.use {
                if (it.moveToFirst()) {
                    val reasonStr = it.getString(it.getColumnIndexOrThrow("reason"))
                    log = FlockMortalityLog(
                        id = it.getLong(it.getColumnIndexOrThrow("id")),
                        flockId = it.getLong(it.getColumnIndexOrThrow("flock_id")),
                        flockName = it.getString(it.getColumnIndexOrThrow("flock_name")),
                        count = it.getInt(it.getColumnIndexOrThrow("count")),
                        reason = MortalityReason.fromString(reasonStr),
                        diagnosisHint = it.getString(it.getColumnIndexOrThrow("diagnosis_hint")),
                        dateJalaliStr = it.getString(it.getColumnIndexOrThrow("date_jalali_str")),
                        timestampMillis = it.getLong(it.getColumnIndexOrThrow("timestamp_millis")),
                        notes = it.getString(it.getColumnIndexOrThrow("notes"))
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return log
    }

    fun getAllMortalityLogs(): List<FlockMortalityLog> {
        val db = readableDatabase
        val list = mutableListOf<FlockMortalityLog>()
        try {
            val cursor = db.rawQuery("SELECT * FROM flock_mortality_logs ORDER BY timestamp_millis DESC, id DESC", null)
            cursor.use {
                while (it.moveToNext()) {
                    val reasonStr = it.getString(it.getColumnIndexOrThrow("reason"))
                    list.add(
                        FlockMortalityLog(
                            id = it.getLong(it.getColumnIndexOrThrow("id")),
                            flockId = it.getLong(it.getColumnIndexOrThrow("flock_id")),
                            flockName = it.getString(it.getColumnIndexOrThrow("flock_name")),
                            count = it.getInt(it.getColumnIndexOrThrow("count")),
                            reason = MortalityReason.fromString(reasonStr),
                            diagnosisHint = it.getString(it.getColumnIndexOrThrow("diagnosis_hint")),
                            dateJalaliStr = it.getString(it.getColumnIndexOrThrow("date_jalali_str")),
                            timestampMillis = it.getLong(it.getColumnIndexOrThrow("timestamp_millis")),
                            notes = it.getString(it.getColumnIndexOrThrow("notes"))
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun deleteMortalityLog(id: Long): Boolean {
        val db = writableDatabase
        val log = getMortalityLogById(id)
        val rows = db.delete("flock_mortality_logs", "id = ?", arrayOf(id.toString()))
        if (rows > 0 && log != null && log.count > 0) {
            // Restore bird count to flock and its subGroups
            val flock = getFlockById(log.flockId)
            if (flock != null) {
                val restoredBirdCount = flock.birdCount + log.count
                val restoredSubGroups = if (flock.subGroups.isNotEmpty()) {
                    val newSubList = flock.subGroups.toMutableList()
                    val targetIdx = newSubList.indices.maxByOrNull { newSubList[it].birdCount } ?: 0
                    newSubList[targetIdx] = newSubList[targetIdx].copy(
                        birdCount = newSubList[targetIdx].birdCount + log.count
                    )
                    newSubList
                } else {
                    emptyList()
                }
                updateFlock(flock.copy(birdCount = restoredBirdCount, subGroups = restoredSubGroups))
            }
        }
        notifyDataChanged()
        return rows > 0
    }

    // ==================== BIRD SALE & SLAUGHTER LOGS ====================

    private fun getFlockByIdWithDb(db: SQLiteDatabase, id: Long): Flock? {
        val cursor = db.rawQuery("SELECT * FROM flocks WHERE id = ?", arrayOf(id.toString()))
        cursor.use {
            if (it.moveToFirst()) {
                val createdAt = it.getLong(it.getColumnIndexOrThrow("created_at_millis"))
                val birthEpoch = it.getLong(it.getColumnIndexOrThrow("birth_epoch_millis"))
                val sgIdx = it.getColumnIndex("subgroups_json")
                val subGroups = if (sgIdx != -1 && !it.isNull(sgIdx)) parseSubGroups(it.getString(sgIdx), createdAt, birthEpoch) else emptyList()
                val feedEnIdx = it.getColumnIndex("is_feed_enabled")
                val isFeedEnabled = if (feedEnIdx != -1 && !it.isNull(feedEnIdx)) it.getInt(feedEnIdx) == 1 else true
                val waterEnIdx = it.getColumnIndex("is_water_enabled")
                val isWaterEnabled = if (waterEnIdx != -1 && !it.isNull(waterEnIdx)) it.getInt(waterEnIdx) == 1 else true
                val tempIdx = it.getColumnIndex("custom_temp_celsius")
                val customTempCelsius = if (tempIdx != -1 && !it.isNull(tempIdx)) it.getDouble(tempIdx) else null
                val medEndIdx = it.getColumnIndex("medication_withdrawal_end_millis")
                val medEndMillis = if (medEndIdx != -1 && !it.isNull(medEndIdx)) it.getLong(medEndIdx) else null
                val medNameIdx = it.getColumnIndex("active_medication_name")
                val medName = if (medNameIdx != -1 && !it.isNull(medNameIdx)) it.getString(medNameIdx) else null

                return Flock(
                    id = it.getLong(it.getColumnIndexOrThrow("id")),
                    name = it.getString(it.getColumnIndexOrThrow("name")),
                    species = BirdSpecies.fromString(it.getString(it.getColumnIndexOrThrow("species"))),
                    breed = it.getString(it.getColumnIndexOrThrow("breed")),
                    birthEpochMillis = birthEpoch,
                    birdCount = it.getInt(it.getColumnIndexOrThrow("bird_count")),
                    photoUri = it.getString(it.getColumnIndexOrThrow("photo_uri")),
                    notes = it.getString(it.getColumnIndexOrThrow("notes")),
                    createdAtMillis = createdAt,
                    subGroups = subGroups,
                    isFeedEnabled = isFeedEnabled,
                    isWaterEnabled = isWaterEnabled,
                    customTempCelsius = customTempCelsius,
                    medicationWithdrawalEndMillis = medEndMillis,
                    activeMedicationName = medName
                )
            }
        }
        return null
    }

    private fun adjustFlockBirdCountInTx(db: SQLiteDatabase, flockId: Long, deltaCount: Int): Boolean {
        val flock = getFlockByIdWithDb(db, flockId) ?: return false
        if (deltaCount == 0) return true

        val updatedSubGroups: List<FlockSubGroup>
        val newBirdCount: Int

        if (deltaCount < 0) {
            val deductCount = -deltaCount
            if (flock.effectiveBirdCount < deductCount) return false
            if (flock.subGroups.isNotEmpty()) {
                var remaining = deductCount
                val newSubList = flock.subGroups.toMutableList()
                val sortedIndices = newSubList.indices.sortedByDescending { newSubList[it].birdCount }
                for (idx in sortedIndices) {
                    if (remaining <= 0) break
                    val currentCount = newSubList[idx].birdCount
                    val dec = currentCount.coerceAtMost(remaining)
                    newSubList[idx] = newSubList[idx].copy(birdCount = (currentCount - dec).coerceAtLeast(0))
                    remaining -= dec
                }
                updatedSubGroups = newSubList
                newBirdCount = updatedSubGroups.sumOf { it.birdCount }
            } else {
                updatedSubGroups = emptyList()
                newBirdCount = (flock.birdCount - deductCount).coerceAtLeast(0)
            }
        } else {
            if (flock.subGroups.isNotEmpty()) {
                val newSubList = flock.subGroups.toMutableList()
                val targetIdx = newSubList.indices.maxByOrNull { newSubList[it].birdCount } ?: 0
                newSubList[targetIdx] = newSubList[targetIdx].copy(
                    birdCount = newSubList[targetIdx].birdCount + deltaCount
                )
                updatedSubGroups = newSubList
                newBirdCount = updatedSubGroups.sumOf { it.birdCount }
            } else {
                updatedSubGroups = emptyList()
                newBirdCount = flock.birdCount + deltaCount
            }
        }

        val cv = ContentValues().apply {
            put("bird_count", newBirdCount)
            put("subgroups_json", serializeSubGroups(updatedSubGroups))
        }
        return db.update("flocks", cv, "id = ?", arrayOf(flockId.toString())) > 0
    }

    fun insertBirdSaleLog(log: BirdSaleLog, createFinanceTransaction: Boolean = true): Long {
        if (log.birdCount <= 0) return -1L
        val db = writableDatabase
        db.beginTransaction()
        var saleId = -1L
        try {
            val flock = getFlockByIdWithDb(db, log.flockId) ?: return -1L
            if (flock.effectiveBirdCount < log.birdCount) return -1L
            if (!adjustFlockBirdCountInTx(db, log.flockId, -log.birdCount)) return -1L

            var txId: Long? = log.linkedTransactionId
            if (createFinanceTransaction && log.totalRevenueTomans > 0.0 && txId == null) {
                val txTitle = buildString {
                    append("فروش ${com.example.poultryapp.engine.toPersianDigits(log.birdCount)} قطعه از ${log.flockName}")
                    if (!log.buyerName.isNullOrBlank()) {
                        append(" به ${log.buyerName}")
                    }
                }
                val liveW = log.liveWeightKg
                val qty = if (liveW != null && liveW > 0.0) liveW else log.birdCount.toDouble()
                val unit = if (liveW != null && liveW > 0.0) "کیلوگرم" else "قطعه"
                val unitPrice = log.unitPriceTomans ?: if (qty > 0.0) log.totalRevenueTomans / qty else 0.0
                val txCv = ContentValues().apply {
                    put("title", txTitle)
                    put("amount_tomans", log.totalRevenueTomans)
                    put("type", TransactionType.INCOME.name)
                    put("category", TransactionCategory.BIRD_SALE.name)
                    put("date_jalali_str", log.dateJalaliStr)
                    put("timestamp_millis", log.timestampMillis)
                    put("related_flock_id", log.flockId)
                    put("related_flock_name", log.flockName)
                    put("quantity", qty)
                    put("unit", unit)
                    put("unit_price_tomans", unitPrice)
                    put("notes", log.notes)
                }
                val insertedTxId = db.insert("financial_transactions", null, txCv)
                if (insertedTxId > 0L) txId = insertedTxId
            }

            val cv = ContentValues().apply {
                put("flock_id", log.flockId)
                put("flock_name", log.flockName)
                put("bird_count", log.birdCount)
                put("bird_age_days", log.birdAgeDays.coerceAtLeast(1))
                if (log.liveWeightKg != null) put("live_weight_kg", log.liveWeightKg) else putNull("live_weight_kg")
                if (log.unitPriceTomans != null) put("unit_price_tomans", log.unitPriceTomans) else putNull("unit_price_tomans")
                put("total_revenue_tomans", log.totalRevenueTomans.coerceAtLeast(0.0))
                if (!log.buyerName.isNullOrBlank()) put("buyer_name", log.buyerName) else putNull("buyer_name")
                if (txId != null && txId > 0L) put("linked_transaction_id", txId) else putNull("linked_transaction_id")
                put("date_jalali_str", log.dateJalaliStr)
                put("timestamp_millis", log.timestampMillis)
                if (!log.notes.isNullOrBlank()) put("notes", log.notes) else putNull("notes")
            }
            saleId = db.insert("flock_bird_sales", null, cv)
            if (saleId > 0L) {
                db.setTransactionSuccessful()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            saleId = -1L
        } finally {
            db.endTransaction()
        }
        if (saleId > 0L) notifyDataChanged()
        return saleId
    }

    private fun readBirdSaleLogFromCursor(c: android.database.Cursor): BirdSaleLog {
        val lwIdx = c.getColumnIndex("live_weight_kg")
        val upIdx = c.getColumnIndex("unit_price_tomans")
        val bnIdx = c.getColumnIndex("buyer_name")
        val ltIdx = c.getColumnIndex("linked_transaction_id")
        val notesIdx = c.getColumnIndex("notes")
        return BirdSaleLog(
            id = c.getLong(c.getColumnIndexOrThrow("id")),
            flockId = c.getLong(c.getColumnIndexOrThrow("flock_id")),
            flockName = c.getString(c.getColumnIndexOrThrow("flock_name")),
            birdCount = c.getInt(c.getColumnIndexOrThrow("bird_count")),
            birdAgeDays = c.getInt(c.getColumnIndexOrThrow("bird_age_days")),
            liveWeightKg = if (lwIdx != -1 && !c.isNull(lwIdx)) c.getDouble(lwIdx) else null,
            unitPriceTomans = if (upIdx != -1 && !c.isNull(upIdx)) c.getDouble(upIdx) else null,
            totalRevenueTomans = c.getDouble(c.getColumnIndexOrThrow("total_revenue_tomans")),
            buyerName = if (bnIdx != -1 && !c.isNull(bnIdx)) c.getString(bnIdx) else null,
            linkedTransactionId = if (ltIdx != -1 && !c.isNull(ltIdx)) c.getLong(ltIdx) else null,
            dateJalaliStr = c.getString(c.getColumnIndexOrThrow("date_jalali_str")),
            timestampMillis = c.getLong(c.getColumnIndexOrThrow("timestamp_millis")),
            notes = if (notesIdx != -1 && !c.isNull(notesIdx)) c.getString(notesIdx) else null
        )
    }

    private fun getBirdSaleLogByIdWithDb(db: SQLiteDatabase, id: Long): BirdSaleLog? {
        val cursor = db.rawQuery("SELECT * FROM flock_bird_sales WHERE id = ? LIMIT 1", arrayOf(id.toString()))
        cursor.use {
            if (it.moveToFirst()) return readBirdSaleLogFromCursor(it)
        }
        return null
    }

    fun getBirdSaleLogById(id: Long): BirdSaleLog? = getBirdSaleLogByIdWithDb(readableDatabase, id)

    fun getAllBirdSaleLogs(): List<BirdSaleLog> {
        val db = readableDatabase
        val list = mutableListOf<BirdSaleLog>()
        try {
            val cursor = db.rawQuery("SELECT * FROM flock_bird_sales ORDER BY timestamp_millis DESC, id DESC", null)
            cursor.use {
                while (it.moveToNext()) {
                    list.add(readBirdSaleLogFromCursor(it))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun deleteBirdSaleLog(id: Long): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        var deleted = false
        try {
            val log = getBirdSaleLogByIdWithDb(db, id) ?: return false
            val rows = db.delete("flock_bird_sales", "id = ?", arrayOf(id.toString()))
            if (rows > 0) {
                if (log.birdCount > 0) {
                    adjustFlockBirdCountInTx(db, log.flockId, log.birdCount)
                }
                if (log.linkedTransactionId != null && log.linkedTransactionId > 0L) {
                    db.delete("financial_transactions", "id = ?", arrayOf(log.linkedTransactionId.toString()))
                }
                db.setTransactionSuccessful()
                deleted = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            deleted = false
        } finally {
            db.endTransaction()
        }
        if (deleted) notifyDataChanged()
        return deleted
    }

    fun insertBirdSlaughterLog(log: BirdSlaughterLog): Long {
        if (log.birdCount <= 0) return -1L
        val db = writableDatabase
        db.beginTransaction()
        var slaughterId = -1L
        try {
            val flock = getFlockByIdWithDb(db, log.flockId) ?: return -1L
            if (flock.effectiveBirdCount < log.birdCount) return -1L
            if (!adjustFlockBirdCountInTx(db, log.flockId, -log.birdCount)) return -1L

            val cv = ContentValues().apply {
                put("flock_id", log.flockId)
                put("flock_name", log.flockName)
                put("bird_count", log.birdCount)
                put("bird_age_days", log.birdAgeDays.coerceAtLeast(1))
                if (log.liveWeightKg != null) put("live_weight_kg", log.liveWeightKg) else putNull("live_weight_kg")
                if (log.carcassWeightKg != null) put("carcass_weight_kg", log.carcassWeightKg) else putNull("carcass_weight_kg")
                put("date_jalali_str", log.dateJalaliStr)
                put("timestamp_millis", log.timestampMillis)
                if (!log.notes.isNullOrBlank()) put("notes", log.notes) else putNull("notes")
            }
            slaughterId = db.insert("flock_bird_slaughters", null, cv)
            if (slaughterId > 0L) {
                db.setTransactionSuccessful()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            slaughterId = -1L
        } finally {
            db.endTransaction()
        }
        if (slaughterId > 0L) notifyDataChanged()
        return slaughterId
    }

    private fun readBirdSlaughterLogFromCursor(c: android.database.Cursor): BirdSlaughterLog {
        val lwIdx = c.getColumnIndex("live_weight_kg")
        val cwIdx = c.getColumnIndex("carcass_weight_kg")
        val notesIdx = c.getColumnIndex("notes")
        return BirdSlaughterLog(
            id = c.getLong(c.getColumnIndexOrThrow("id")),
            flockId = c.getLong(c.getColumnIndexOrThrow("flock_id")),
            flockName = c.getString(c.getColumnIndexOrThrow("flock_name")),
            birdCount = c.getInt(c.getColumnIndexOrThrow("bird_count")),
            birdAgeDays = c.getInt(c.getColumnIndexOrThrow("bird_age_days")),
            liveWeightKg = if (lwIdx != -1 && !c.isNull(lwIdx)) c.getDouble(lwIdx) else null,
            carcassWeightKg = if (cwIdx != -1 && !c.isNull(cwIdx)) c.getDouble(cwIdx) else null,
            dateJalaliStr = c.getString(c.getColumnIndexOrThrow("date_jalali_str")),
            timestampMillis = c.getLong(c.getColumnIndexOrThrow("timestamp_millis")),
            notes = if (notesIdx != -1 && !c.isNull(notesIdx)) c.getString(notesIdx) else null
        )
    }

    private fun getBirdSlaughterLogByIdWithDb(db: SQLiteDatabase, id: Long): BirdSlaughterLog? {
        val cursor = db.rawQuery("SELECT * FROM flock_bird_slaughters WHERE id = ? LIMIT 1", arrayOf(id.toString()))
        cursor.use {
            if (it.moveToFirst()) return readBirdSlaughterLogFromCursor(it)
        }
        return null
    }

    fun getBirdSlaughterLogById(id: Long): BirdSlaughterLog? = getBirdSlaughterLogByIdWithDb(readableDatabase, id)

    fun getAllBirdSlaughterLogs(): List<BirdSlaughterLog> {
        val db = readableDatabase
        val list = mutableListOf<BirdSlaughterLog>()
        try {
            val cursor = db.rawQuery("SELECT * FROM flock_bird_slaughters ORDER BY timestamp_millis DESC, id DESC", null)
            cursor.use {
                while (it.moveToNext()) {
                    list.add(readBirdSlaughterLogFromCursor(it))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun deleteBirdSlaughterLog(id: Long): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        var deleted = false
        try {
            val log = getBirdSlaughterLogByIdWithDb(db, id) ?: return false
            val rows = db.delete("flock_bird_slaughters", "id = ?", arrayOf(id.toString()))
            if (rows > 0) {
                if (log.birdCount > 0) {
                    adjustFlockBirdCountInTx(db, log.flockId, log.birdCount)
                }
                db.setTransactionSuccessful()
                deleted = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            deleted = false
        } finally {
            db.endTransaction()
        }
        if (deleted) notifyDataChanged()
        return deleted
    }

    // ==================== FARM COMMERCIAL CONTACTS ====================

    fun insertContact(contact: FarmContact): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("full_name", contact.fullName)
            put("business_name", contact.businessName)
            put("category", contact.category.name)
            put("phone_primary", contact.phonePrimary)
            put("phone_secondary", contact.phoneSecondary)
            put("address", contact.address)
            put("province_city", contact.provinceCity)
            put("balance_tomans", contact.balanceTomans)
            put("payment_terms_notes", contact.paymentTermsNotes)
            put("rating_stars", contact.ratingStars)
            put("created_at_millis", contact.createdAtMillis)
        }
        val id = db.insert("farm_contacts", null, cv)
        notifyDataChanged()
        return id
    }

    fun updateContact(contact: FarmContact): Boolean {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("full_name", contact.fullName)
            put("business_name", contact.businessName)
            put("category", contact.category.name)
            put("phone_primary", contact.phonePrimary)
            put("phone_secondary", contact.phoneSecondary)
            put("address", contact.address)
            put("province_city", contact.provinceCity)
            put("balance_tomans", contact.balanceTomans)
            put("payment_terms_notes", contact.paymentTermsNotes)
            put("rating_stars", contact.ratingStars)
        }
        val rows = db.update("farm_contacts", cv, "id = ?", arrayOf(contact.id.toString()))
        notifyDataChanged()
        return rows > 0
    }

    fun deleteContact(id: Long): Boolean {
        val db = writableDatabase
        val rows = db.delete("farm_contacts", "id = ?", arrayOf(id.toString()))
        notifyDataChanged()
        return rows > 0
    }

    fun getAllContacts(): List<FarmContact> {
        val db = readableDatabase
        val list = mutableListOf<FarmContact>()
        try {
            val cursor = db.rawQuery("SELECT * FROM farm_contacts ORDER BY id DESC", null)
            cursor.use {
                while (it.moveToNext()) {
                    val catStr = it.getString(it.getColumnIndexOrThrow("category"))
                    list.add(
                        FarmContact(
                            id = it.getLong(it.getColumnIndexOrThrow("id")),
                            fullName = it.getString(it.getColumnIndexOrThrow("full_name")),
                            businessName = it.getString(it.getColumnIndexOrThrow("business_name")),
                            category = ContactCategory.fromString(catStr),
                            phonePrimary = it.getString(it.getColumnIndexOrThrow("phone_primary")),
                            phoneSecondary = it.getString(it.getColumnIndexOrThrow("phone_secondary")),
                            address = it.getString(it.getColumnIndexOrThrow("address")),
                            provinceCity = it.getString(it.getColumnIndexOrThrow("province_city")),
                            balanceTomans = it.getDouble(it.getColumnIndexOrThrow("balance_tomans")),
                            paymentTermsNotes = it.getString(it.getColumnIndexOrThrow("payment_terms_notes")),
                            ratingStars = it.getInt(it.getColumnIndexOrThrow("rating_stars")),
                            createdAtMillis = it.getLong(it.getColumnIndexOrThrow("created_at_millis"))
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun getEggProductionsForFlock(flockId: Long): List<EggProduction> {
        return getAllEggProductions().filter { it.flockId == flockId }
    }

    fun deleteEggProduction(id: Long): Boolean {
        val db = writableDatabase
        val rows = db.delete("egg_productions", "id = ?", arrayOf(id.toString()))
        notifyDataChanged()
        return rows > 0
    }

    // ==================== FINANCIAL TRANSACTIONS & WAREHOUSE ACTIONS ====================

    fun insertFinancialTransaction(tx: FinancialTransaction): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("title", tx.title)
            put("amount_tomans", tx.amountTomans)
            put("type", tx.type.name)
            put("category", tx.category.name)
            put("date_jalali_str", tx.dateJalaliStr)
            put("timestamp_millis", tx.timestampMillis)
            if (tx.relatedFlockId != null) put("related_flock_id", tx.relatedFlockId)
            if (tx.relatedFlockName != null) put("related_flock_name", tx.relatedFlockName)
            if (tx.relatedWarehouseItemId != null) put("related_warehouse_item_id", tx.relatedWarehouseItemId)
            put("quantity", tx.quantity)
            put("unit", tx.unit)
            put("unit_price_tomans", tx.unitPriceTomans)
            put("notes", tx.notes)
        }
        val id = db.insert("financial_transactions", null, cv)
        notifyDataChanged()
        return id
    }

    fun getAllFinancialTransactions(): List<FinancialTransaction> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM financial_transactions ORDER BY timestamp_millis DESC, id DESC", null)
        val list = mutableListOf<FinancialTransaction>()
        cursor.use {
            while (it.moveToNext()) {
                val flockIdIdx = it.getColumnIndex("related_flock_id")
                val flockNameIdx = it.getColumnIndex("related_flock_name")
                val itemIdIdx = it.getColumnIndex("related_warehouse_item_id")
                list.add(
                    FinancialTransaction(
                        id = it.getLong(it.getColumnIndexOrThrow("id")),
                        title = it.getString(it.getColumnIndexOrThrow("title")),
                        amountTomans = it.getDouble(it.getColumnIndexOrThrow("amount_tomans")),
                        type = TransactionType.valueOf(it.getString(it.getColumnIndexOrThrow("type"))),
                        category = TransactionCategory.fromString(it.getString(it.getColumnIndexOrThrow("category"))),
                        dateJalaliStr = it.getString(it.getColumnIndexOrThrow("date_jalali_str")),
                        timestampMillis = it.getLong(it.getColumnIndexOrThrow("timestamp_millis")),
                        relatedFlockId = if (flockIdIdx != -1 && !it.isNull(flockIdIdx)) it.getLong(flockIdIdx) else null,
                        relatedFlockName = if (flockNameIdx != -1 && !it.isNull(flockNameIdx)) it.getString(flockNameIdx) else null,
                        relatedWarehouseItemId = if (itemIdIdx != -1 && !it.isNull(itemIdIdx)) it.getLong(itemIdIdx) else null,
                        quantity = it.getDouble(it.getColumnIndexOrThrow("quantity")),
                        unit = it.getString(it.getColumnIndexOrThrow("unit")),
                        unitPriceTomans = it.getDouble(it.getColumnIndexOrThrow("unit_price_tomans")),
                        notes = it.getString(it.getColumnIndexOrThrow("notes"))
                    )
                )
            }
        }
        return list
    }

    fun deleteFinancialTransaction(id: Long): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        val rows = try {
            val clearCv = ContentValues().apply { putNull("linked_transaction_id") }
            db.update("flock_bird_sales", clearCv, "linked_transaction_id = ?", arrayOf(id.toString()))
            val deleted = db.delete("financial_transactions", "id = ?", arrayOf(id.toString()))
            db.setTransactionSuccessful()
            deleted
        } catch (e: Exception) {
            e.printStackTrace()
            0
        } finally {
            db.endTransaction()
        }
        if (rows > 0) notifyDataChanged()
        return rows > 0
    }

    // Warehouse Consumables "مصرف شد"
    fun consumeWarehouseItem(
        itemId: Long,
        amount: Double,
        flockIds: List<Long>,
        dateJalaliStr: String,
        notes: String?
    ): Boolean {
        val item = getWarehouseItemById(itemId) ?: return false
        val newStock = (item.currentStock - amount).coerceAtLeast(0.0)
        updateWarehouseItem(item.copy(currentStock = newStock))

        val cost = amount * item.pricePerUnit
        val selectedFlocks = flockIds.mapNotNull { getFlockById(it) }
        val flockNamesStr = when {
            selectedFlocks.isEmpty() -> null
            selectedFlocks.size == 1 -> selectedFlocks[0].name
            else -> selectedFlocks.joinToString("، ") { it.name }
        }
        val primaryFlockId = if (selectedFlocks.size == 1) selectedFlocks[0].id else null

        val cat = when (item.category) {
            WarehouseCategory.FEED -> TransactionCategory.FEED_CONSUMPTION
            WarehouseCategory.VITAMIN, WarehouseCategory.PROBIOTIC, WarehouseCategory.MEDICINE -> TransactionCategory.MED_SUPPLEMENT
            else -> TransactionCategory.OTHER_EXPENSE
        }

        val txTitle = buildString {
            append("مصرف ${item.name} (${com.example.poultryapp.engine.toPersianDigits(amount.toInt())} ${item.unit})")
            if (!flockNamesStr.isNullOrBlank()) {
                append(" - گله: $flockNamesStr")
            }
        }

        if (cost > 0) {
            insertFinancialTransaction(
                FinancialTransaction(
                    title = txTitle,
                    amountTomans = cost,
                    type = TransactionType.EXPENSE,
                    category = cat,
                    dateJalaliStr = dateJalaliStr,
                    relatedFlockId = primaryFlockId,
                    relatedFlockName = flockNamesStr,
                    relatedWarehouseItemId = item.id,
                    quantity = amount,
                    unit = item.unit,
                    unitPriceTomans = item.pricePerUnit,
                    notes = notes
                )
            )
        }
        notifyDataChanged()
        return true
    }

    fun consumeWarehouseItem(
        itemId: Long,
        amount: Double,
        flockId: Long?,
        dateJalaliStr: String,
        notes: String?
    ): Boolean = consumeWarehouseItem(itemId, amount, flockId?.let { listOf(it) } ?: emptyList(), dateJalaliStr, notes)

    // Warehouse Egg Sales "فروش رفت"
    fun sellEggItem(
        itemId: Long,
        count: Double,
        pricePerUnit: Double,
        dateJalaliStr: String,
        notes: String?
    ): Boolean {
        val item = getWarehouseItemById(itemId) ?: return false
        val newStock = (item.currentStock - count).coerceAtLeast(0.0)
        updateWarehouseItem(item.copy(
            currentStock = newStock,
            pricePerUnit = pricePerUnit
        ))

        val revenue = count * pricePerUnit
        if (revenue > 0) {
            insertFinancialTransaction(
                FinancialTransaction(
                    title = "فروش ${item.name} (${com.example.poultryapp.engine.toPersianDigits(count.toInt())} عدد)",
                    amountTomans = revenue,
                    type = TransactionType.INCOME,
                    category = TransactionCategory.EGG_SALE,
                    dateJalaliStr = dateJalaliStr,
                    relatedWarehouseItemId = item.id,
                    quantity = count,
                    unit = "عدد",
                    unitPriceTomans = pricePerUnit,
                    notes = notes
                )
            )
        }
        notifyDataChanged()
        return true
    }

    // ==================== SETTINGS ====================

    fun getSetting(key: String, defaultValue: String): String {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT setting_value FROM app_settings WHERE setting_key = ?", arrayOf(key))
        cursor.use {
            if (it.moveToFirst()) {
                return it.getString(0)
            }
        }
        return defaultValue
    }

    fun saveSetting(key: String, value: String) {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("setting_key", key)
            put("setting_value", value)
        }
        db.insertWithOnConflict("app_settings", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        notifyDataChanged()
    }

    // ==================== CUSTOM BREEDS HELPER & CRUD ====================

    data class ParsedBreedRates(
        val mode: String,
        val weeklyRates: List<WeeklyConsumptionRate>,
        val dailyRates: List<DailyConsumptionRate>
    )

    fun serializeBreedRates(
        mode: String,
        weeklyRates: List<WeeklyConsumptionRate>,
        dailyRates: List<DailyConsumptionRate>
    ): String {
        val root = JSONObject().apply {
            put("mode", mode)
            val wArr = JSONArray()
            for (r in weeklyRates) {
                wArr.put(JSONObject().apply {
                    put("week", r.week)
                    put("dailyFeedGrams", r.dailyFeedGrams)
                    put("dailyWaterMl", r.dailyWaterMl)
                })
            }
            put("weeklyRates", wArr)

            val dArr = JSONArray()
            for (d in dailyRates) {
                dArr.put(JSONObject().apply {
                    put("day", d.day)
                    put("dailyFeedGrams", d.dailyFeedGrams)
                    put("dailyWaterMl", d.dailyWaterMl)
                })
            }
            put("dailyRates", dArr)
        }
        return root.toString()
    }

    fun parseBreedRates(jsonStr: String?): ParsedBreedRates {
        if (jsonStr.isNullOrBlank()) return ParsedBreedRates("WEEKS", emptyList(), emptyList())
        val clean = jsonStr.trim()
        return try {
            if (clean.startsWith("{")) {
                val obj = JSONObject(clean)
                val mode = obj.optString("mode", "WEEKS")
                val wList = mutableListOf<WeeklyConsumptionRate>()
                if (obj.has("weeklyRates")) {
                    val wArr = obj.getJSONArray("weeklyRates")
                    for (i in 0 until wArr.length()) {
                        val item = wArr.getJSONObject(i)
                        wList.add(
                            WeeklyConsumptionRate(
                                week = item.getInt("week"),
                                dailyFeedGrams = item.getDouble("dailyFeedGrams"),
                                dailyWaterMl = item.getDouble("dailyWaterMl")
                            )
                        )
                    }
                }
                val dList = mutableListOf<DailyConsumptionRate>()
                if (obj.has("dailyRates")) {
                    val dArr = obj.getJSONArray("dailyRates")
                    for (i in 0 until dArr.length()) {
                        val item = dArr.getJSONObject(i)
                        dList.add(
                            DailyConsumptionRate(
                                day = item.getInt("day"),
                                dailyFeedGrams = item.getDouble("dailyFeedGrams"),
                                dailyWaterMl = item.getDouble("dailyWaterMl")
                            )
                        )
                    }
                }
                ParsedBreedRates(mode, wList.sortedBy { it.week }, dList.sortedBy { it.day })
            } else if (clean.startsWith("[")) {
                val list = parseWeeklyRates(clean)
                ParsedBreedRates("WEEKS", list, emptyList())
            } else {
                ParsedBreedRates("WEEKS", emptyList(), emptyList())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ParsedBreedRates("WEEKS", emptyList(), emptyList())
        }
    }

    fun serializeWeeklyRates(rates: List<WeeklyConsumptionRate>): String {
        return serializeBreedRates("WEEKS", rates, emptyList())
    }

    fun parseWeeklyRates(jsonStr: String?): List<WeeklyConsumptionRate> {
        if (jsonStr.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<WeeklyConsumptionRate>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    WeeklyConsumptionRate(
                        week = obj.getInt("week"),
                        dailyFeedGrams = obj.getDouble("dailyFeedGrams"),
                        dailyWaterMl = obj.getDouble("dailyWaterMl")
                    )
                )
            }
            list.sortedBy { it.week }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun insertCustomBreed(breed: CustomBreed): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("name", breed.name.trim())
            put("species", breed.species.name)
            put("production_type", breed.productionType.name)
            put("adult_feed_grams", breed.adultFeedGrams)
            put("adult_water_ml", breed.adultWaterMl)
            put("weekly_rates_json", serializeBreedRates(breed.rateInputMode, breed.weeklyRates, breed.dailyRates))
            put("notes", breed.notes)
        }
        val id = db.insert("custom_breeds", null, cv)
        if (id != -1L) notifyDataChanged()
        return id
    }

    fun updateCustomBreed(breed: CustomBreed): Boolean {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("name", breed.name.trim())
            put("species", breed.species.name)
            put("production_type", breed.productionType.name)
            put("adult_feed_grams", breed.adultFeedGrams)
            put("adult_water_ml", breed.adultWaterMl)
            put("weekly_rates_json", serializeBreedRates(breed.rateInputMode, breed.weeklyRates, breed.dailyRates))
            put("notes", breed.notes)
        }
        val rows = db.update("custom_breeds", cv, "id = ?", arrayOf(breed.id.toString()))
        if (rows > 0) notifyDataChanged()
        return rows > 0
    }

    fun deleteCustomBreed(id: Long): Boolean {
        val db = writableDatabase
        val rows = db.delete("custom_breeds", "id = ?", arrayOf(id.toString()))
        if (rows > 0) notifyDataChanged()
        return rows > 0
    }

    fun getAllCustomBreeds(): List<CustomBreed> {
        val db = readableDatabase
        val list = mutableListOf<CustomBreed>()
        val cursor = db.query(
            "custom_breeds",
            null,
            null,
            null,
            null,
            null,
            "id ASC"
        )
        cursor.use {
            val idCol = it.getColumnIndexOrThrow("id")
            val nameCol = it.getColumnIndexOrThrow("name")
            val speciesCol = it.getColumnIndexOrThrow("species")
            val prodTypeCol = it.getColumnIndexOrThrow("production_type")
            val adultFeedCol = it.getColumnIndexOrThrow("adult_feed_grams")
            val adultWaterCol = it.getColumnIndexOrThrow("adult_water_ml")
            val weeklyRatesCol = it.getColumnIndexOrThrow("weekly_rates_json")
            val notesCol = it.getColumnIndexOrThrow("notes")

            while (it.moveToNext()) {
                val speciesStr = it.getString(speciesCol)
                val species = try {
                    BirdSpecies.valueOf(speciesStr)
                } catch (_: Exception) {
                    BirdSpecies.CHICKEN
                }
                val prodTypeStr = it.getString(prodTypeCol)
                val prodType = try {
                    ProductionType.valueOf(prodTypeStr)
                } catch (_: Exception) {
                    ProductionType.DUAL
                }
                val parsed = parseBreedRates(it.getString(weeklyRatesCol))
                list.add(
                    CustomBreed(
                        id = it.getLong(idCol),
                        name = it.getString(nameCol),
                        species = species,
                        productionType = prodType,
                        adultFeedGrams = it.getDouble(adultFeedCol),
                        adultWaterMl = it.getDouble(adultWaterCol),
                        weeklyRates = parsed.weeklyRates,
                        dailyRates = parsed.dailyRates,
                        rateInputMode = parsed.mode,
                        notes = if (it.isNull(notesCol)) null else it.getString(notesCol)
                    )
                )
            }
        }
        return list
    }

    fun getCustomBreedById(id: Long): CustomBreed? {
        val db = readableDatabase
        val cursor = db.query(
            "custom_breeds",
            null,
            "id = ?",
            arrayOf(id.toString()),
            null,
            null,
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                val idCol = it.getColumnIndexOrThrow("id")
                val nameCol = it.getColumnIndexOrThrow("name")
                val speciesCol = it.getColumnIndexOrThrow("species")
                val prodTypeCol = it.getColumnIndexOrThrow("production_type")
                val adultFeedCol = it.getColumnIndexOrThrow("adult_feed_grams")
                val adultWaterCol = it.getColumnIndexOrThrow("adult_water_ml")
                val weeklyRatesCol = it.getColumnIndexOrThrow("weekly_rates_json")
                val notesCol = it.getColumnIndexOrThrow("notes")

                val species = try {
                    BirdSpecies.valueOf(it.getString(speciesCol))
                } catch (_: Exception) {
                    BirdSpecies.CHICKEN
                }
                val prodType = try {
                    ProductionType.valueOf(it.getString(prodTypeCol))
                } catch (_: Exception) {
                    ProductionType.DUAL
                }
                val parsed = parseBreedRates(it.getString(weeklyRatesCol))
                return CustomBreed(
                    id = it.getLong(idCol),
                    name = it.getString(nameCol),
                    species = species,
                    productionType = prodType,
                    adultFeedGrams = it.getDouble(adultFeedCol),
                    adultWaterMl = it.getDouble(adultWaterCol),
                    weeklyRates = parsed.weeklyRates,
                    dailyRates = parsed.dailyRates,
                    rateInputMode = parsed.mode,
                    notes = if (it.isNull(notesCol)) null else it.getString(notesCol)
                )
            }
        }
        return null
    }

    // ==================== BACKUP & RESTORE (JSON) ====================

    fun exportDatabaseToJson(): String {
        val root = JSONObject()
        root.put("version", 3)
        root.put("company", "ProFarm")
        root.put("exportTimestamp", System.currentTimeMillis())

        // 1. Flocks
        val flocksArr = JSONArray()
        for (f in getAllFlocks()) {
            val fo = JSONObject().apply {
                put("id", f.id)
                put("name", f.name)
                put("species", f.species.name)
                put("breed", f.breed)
                put("birthEpochMillis", f.birthEpochMillis)
                put("birdCount", f.effectiveBirdCount)
                put("photoUri", f.photoUri ?: "")
                put("notes", f.notes ?: "")
                put("createdAtMillis", f.createdAtMillis)
                put("subgroupsJson", serializeSubGroups(f.subGroups))
                put("isFeedEnabled", f.isFeedEnabled)
                put("isWaterEnabled", f.isWaterEnabled)
                if (f.customTempCelsius != null) put("customTempCelsius", f.customTempCelsius)
            }
            flocksArr.put(fo)
        }
        root.put("flocks", flocksArr)

        // 2. Custom Breeds
        val cbArr = JSONArray()
        for (cb in getAllCustomBreeds()) {
            val cbo = JSONObject().apply {
                put("id", cb.id)
                put("name", cb.name)
                put("species", cb.species.name)
                put("productionType", cb.productionType.name)
                put("adultFeedGrams", cb.adultFeedGrams)
                put("adultWaterMl", cb.adultWaterMl)
                put("weeklyRatesJson", serializeBreedRates(cb.rateInputMode, cb.weeklyRates, cb.dailyRates))
                put("notes", cb.notes ?: "")
            }
            cbArr.put(cbo)
        }
        root.put("customBreeds", cbArr)

        // 3. Schedules
        val schedArr = JSONArray()
        for (s in getAllSchedules()) {
            val so = JSONObject().apply {
                put("id", s.id)
                put("flockId", s.flockId ?: -1L)
                put("flockName", s.flockName ?: "")
                put("scheduleType", s.scheduleType)
                put("title", s.title)
                put("notes", s.notes ?: "")
                put("daysOfWeek", s.daysOfWeek.joinToString(","))
                put("hour", s.hour)
                put("minute", s.minute)
                put("isEnabled", s.isEnabled)
                put("scheduleMode", s.scheduleMode)
                if (s.dayOfMonth != null) put("dayOfMonth", s.dayOfMonth)
            }
            schedArr.put(so)
        }
        root.put("schedules", schedArr)

        // 4. Warehouse Items (With Assigned Flock IDs)
        val whArr = JSONArray()
        for (w in getAllWarehouseItems()) {
            val wo = JSONObject().apply {
                put("id", w.id)
                put("name", w.name)
                put("category", w.category.name)
                put("currentStock", w.currentStock)
                put("unit", w.unit)
                put("assignedFlockIds", w.assignedFlockIds.joinToString(","))
                put("pricePerUnit", w.pricePerUnit)
                put("minAlertThreshold", w.minAlertThreshold)
                put("isForSale", w.isForSale)
                put("notes", w.notes ?: "")
                put("updatedAtMillis", w.updatedAtMillis)
            }
            whArr.put(wo)
        }
        root.put("warehouseItems", whArr)

        // 5. Feed & Water Charges (Complete Live Monitoring Records)
        val chargesArr = JSONArray()
        val db = readableDatabase
        val chargesCursor = db.rawQuery(
            "SELECT id, flock_id, item_type, charged_amount, charge_timestamp_millis, date_jalali_str, notes FROM feed_water_charges ORDER BY charge_timestamp_millis ASC",
            null
        )
        chargesCursor.use {
            while (it.moveToNext()) {
                val co = JSONObject().apply {
                    put("id", it.getLong(0))
                    put("flockId", it.getLong(1))
                    put("itemType", it.getString(2))
                    put("chargedAmount", it.getDouble(3))
                    put("chargeTimestampMillis", it.getLong(4))
                    put("dateJalaliStr", it.getString(5))
                    if (!it.isNull(6)) put("notes", it.getString(6))
                }
                chargesArr.put(co)
            }
        }
        root.put("feedWaterCharges", chargesArr)

        // 6. Farm & App Settings
        val settingsObj = JSONObject()
        val settingsCursor = db.rawQuery("SELECT setting_key, setting_value FROM app_settings", null)
        settingsCursor.use {
            while (it.moveToNext()) {
                settingsObj.put(it.getString(0), it.getString(1))
            }
        }
        root.put("appSettings", settingsObj)

        // 7. Egg Productions
        val eggArr = JSONArray()
        val eggCursor = db.rawQuery("SELECT * FROM egg_productions", null)
        eggCursor.use {
            while (it.moveToNext()) {
                val o = JSONObject().apply {
                    put("id", it.getLong(it.getColumnIndexOrThrow("id")))
                    put("flockId", it.getLong(it.getColumnIndexOrThrow("flock_id")))
                    put("flockName", it.getString(it.getColumnIndexOrThrow("flock_name")))
                    put("healthyCount", it.getInt(it.getColumnIndexOrThrow("healthy_count")))
                    put("brokenCount", it.getInt(it.getColumnIndexOrThrow("broken_count")))
                    put("dirtyCount", it.getInt(it.getColumnIndexOrThrow("dirty_count")))
                    put("doubleYolkCount", it.getInt(it.getColumnIndexOrThrow("double_yolk_count")))
                    put("totalCount", it.getInt(it.getColumnIndexOrThrow("total_count")))
                    put("layingRatePercent", it.getDouble(it.getColumnIndexOrThrow("laying_rate_percent")))
                    put("dateJalaliStr", it.getString(it.getColumnIndexOrThrow("date_jalali_str")))
                    put("timestampMillis", it.getLong(it.getColumnIndexOrThrow("timestamp_millis")))
                    val nIdx = it.getColumnIndex("notes")
                    if (nIdx != -1 && !it.isNull(nIdx)) put("notes", it.getString(nIdx))
                }
                eggArr.put(o)
            }
        }
        root.put("eggProductions", eggArr)

        // 8. Financial Transactions
        val finArr = JSONArray()
        val finCursor = db.rawQuery("SELECT * FROM financial_transactions", null)
        finCursor.use {
            while (it.moveToNext()) {
                val o = JSONObject().apply {
                    put("id", it.getLong(it.getColumnIndexOrThrow("id")))
                    put("title", it.getString(it.getColumnIndexOrThrow("title")))
                    put("amountTomans", it.getDouble(it.getColumnIndexOrThrow("amount_tomans")))
                    put("type", it.getString(it.getColumnIndexOrThrow("type")))
                    put("category", it.getString(it.getColumnIndexOrThrow("category")))
                    put("dateJalaliStr", it.getString(it.getColumnIndexOrThrow("date_jalali_str")))
                    put("timestampMillis", it.getLong(it.getColumnIndexOrThrow("timestamp_millis")))
                    val rFlockId = it.getColumnIndex("related_flock_id")
                    if (rFlockId != -1 && !it.isNull(rFlockId)) put("relatedFlockId", it.getLong(rFlockId))
                    val rFlockName = it.getColumnIndex("related_flock_name")
                    if (rFlockName != -1 && !it.isNull(rFlockName)) put("relatedFlockName", it.getString(rFlockName))
                    val rItemId = it.getColumnIndex("related_warehouse_item_id")
                    if (rItemId != -1 && !it.isNull(rItemId)) put("relatedWarehouseItemId", it.getLong(rItemId))
                    put("quantity", it.getDouble(it.getColumnIndexOrThrow("quantity")))
                    put("unit", it.getString(it.getColumnIndexOrThrow("unit")))
                    put("unitPriceTomans", it.getDouble(it.getColumnIndexOrThrow("unit_price_tomans")))
                    val nIdx = it.getColumnIndex("notes")
                    if (nIdx != -1 && !it.isNull(nIdx)) put("notes", it.getString(nIdx))
                }
                finArr.put(o)
            }
        }
        root.put("financialTransactions", finArr)

        // 9. Bird Transfers
        val transArr = JSONArray()
        val transCursor = db.rawQuery("SELECT * FROM bird_transfers", null)
        transCursor.use {
            while (it.moveToNext()) {
                val o = JSONObject().apply {
                    put("id", it.getLong(it.getColumnIndexOrThrow("id")))
                    put("fromFlockId", it.getLong(it.getColumnIndexOrThrow("from_flock_id")))
                    put("fromFlockName", it.getString(it.getColumnIndexOrThrow("from_flock_name")))
                    put("toFlockId", it.getLong(it.getColumnIndexOrThrow("to_flock_id")))
                    put("toFlockName", it.getString(it.getColumnIndexOrThrow("to_flock_name")))
                    put("birdCount", it.getInt(it.getColumnIndexOrThrow("bird_count")))
                    put("dateJalaliStr", it.getString(it.getColumnIndexOrThrow("date_jalali_str")))
                    put("timestampMillis", it.getLong(it.getColumnIndexOrThrow("timestamp_millis")))
                    val nIdx = it.getColumnIndex("notes")
                    if (nIdx != -1 && !it.isNull(nIdx)) put("notes", it.getString(nIdx))
                }
                transArr.put(o)
            }
        }
        root.put("birdTransfers", transArr)

        // 10. Ration Plans
        val rationArr = JSONArray()
        val rationCursor = db.rawQuery("SELECT * FROM ration_plans", null)
        rationCursor.use {
            while (it.moveToNext()) {
                val o = JSONObject().apply {
                    put("id", it.getLong(it.getColumnIndexOrThrow("id")))
                    put("name", it.getString(it.getColumnIndexOrThrow("name")))
                    put("itemType", it.getString(it.getColumnIndexOrThrow("item_type")))
                    put("targetBird", it.getString(it.getColumnIndexOrThrow("target_bird")))
                    put("targetStage", it.getString(it.getColumnIndexOrThrow("target_stage")))
                    put("pricingMode", it.getString(it.getColumnIndexOrThrow("pricing_mode")))
                    put("manualPricePerKg", it.getDouble(it.getColumnIndexOrThrow("manual_price_per_kg")))
                    put("batchBaseKg", it.getDouble(it.getColumnIndexOrThrow("batch_base_kg")))
                    put("ingredientsJson", it.getString(it.getColumnIndexOrThrow("ingredients_json")))
                    val nIdx = it.getColumnIndex("notes")
                    if (nIdx != -1 && !it.isNull(nIdx)) put("notes", it.getString(nIdx))
                    put("createdAtMillis", it.getLong(it.getColumnIndexOrThrow("created_at_millis")))
                    put("updatedAtMillis", it.getLong(it.getColumnIndexOrThrow("updated_at_millis")))
                }
                rationArr.put(o)
            }
        }
        root.put("rationPlans", rationArr)

        // 11. Farm Contacts
        val contactsArr = JSONArray()
        val contactsCursor = db.rawQuery("SELECT * FROM farm_contacts ORDER BY id ASC", null)
        contactsCursor.use {
            while (it.moveToNext()) {
                val o = JSONObject().apply {
                    put("id", it.getLong(it.getColumnIndexOrThrow("id")))
                    put("fullName", it.getString(it.getColumnIndexOrThrow("full_name")))
                    val bnIdx = it.getColumnIndex("business_name")
                    if (bnIdx != -1 && !it.isNull(bnIdx)) put("businessName", it.getString(bnIdx))
                    put("category", it.getString(it.getColumnIndexOrThrow("category")))
                    put("phonePrimary", it.getString(it.getColumnIndexOrThrow("phone_primary")))
                    val psIdx = it.getColumnIndex("phone_secondary")
                    if (psIdx != -1 && !it.isNull(psIdx)) put("phoneSecondary", it.getString(psIdx))
                    val addrIdx = it.getColumnIndex("address")
                    if (addrIdx != -1 && !it.isNull(addrIdx)) put("address", it.getString(addrIdx))
                    val pcIdx = it.getColumnIndex("province_city")
                    if (pcIdx != -1 && !it.isNull(pcIdx)) put("provinceCity", it.getString(pcIdx))
                    put("balanceTomans", it.getDouble(it.getColumnIndexOrThrow("balance_tomans")))
                    val ptnIdx = it.getColumnIndex("payment_terms_notes")
                    if (ptnIdx != -1 && !it.isNull(ptnIdx)) put("paymentTermsNotes", it.getString(ptnIdx))
                    put("ratingStars", it.getInt(it.getColumnIndexOrThrow("rating_stars")))
                    put("createdAtMillis", it.getLong(it.getColumnIndexOrThrow("created_at_millis")))
                }
                contactsArr.put(o)
            }
        }
        root.put("farmContacts", contactsArr)

        // 12. Flock Mortality Logs
        val mortArr = JSONArray()
        val mortCursor = db.rawQuery("SELECT * FROM flock_mortality_logs ORDER BY id ASC", null)
        mortCursor.use {
            while (it.moveToNext()) {
                val o = JSONObject().apply {
                    put("id", it.getLong(it.getColumnIndexOrThrow("id")))
                    put("flockId", it.getLong(it.getColumnIndexOrThrow("flock_id")))
                    put("flockName", it.getString(it.getColumnIndexOrThrow("flock_name")))
                    put("count", it.getInt(it.getColumnIndexOrThrow("count")))
                    put("reason", it.getString(it.getColumnIndexOrThrow("reason")))
                    val dhIdx = it.getColumnIndex("diagnosis_hint")
                    if (dhIdx != -1 && !it.isNull(dhIdx)) put("diagnosisHint", it.getString(dhIdx))
                    put("dateJalaliStr", it.getString(it.getColumnIndexOrThrow("date_jalali_str")))
                    put("timestampMillis", it.getLong(it.getColumnIndexOrThrow("timestamp_millis")))
                    val nIdx = it.getColumnIndex("notes")
                    if (nIdx != -1 && !it.isNull(nIdx)) put("notes", it.getString(nIdx))
                }
                mortArr.put(o)
            }
        }
        root.put("flockMortalityLogs", mortArr)

        // 13. Flock Observations
        val obsArr = JSONArray()
        val obsCursor = db.rawQuery("SELECT * FROM flock_observations ORDER BY id ASC", null)
        obsCursor.use {
            while (it.moveToNext()) {
                val o = JSONObject().apply {
                    put("id", it.getLong(it.getColumnIndexOrThrow("id")))
                    put("flockId", it.getLong(it.getColumnIndexOrThrow("flock_id")))
                    put("flockName", it.getString(it.getColumnIndexOrThrow("flock_name")))
                    put("note", it.getString(it.getColumnIndexOrThrow("note")))
                    put("authorRole", it.getString(it.getColumnIndexOrThrow("author_role")))
                    put("category", it.getString(it.getColumnIndexOrThrow("category")))
                    put("dateJalaliStr", it.getString(it.getColumnIndexOrThrow("date_jalali_str")))
                    put("timestampMillis", it.getLong(it.getColumnIndexOrThrow("timestamp_millis")))
                }
                obsArr.put(o)
            }
        }
        root.put("flockObservations", obsArr)

        // 14. Custom Feed Ingredients
        val feedIngArr = JSONArray()
        val feedIngCursor = db.rawQuery("SELECT * FROM custom_feed_ingredients ORDER BY name ASC", null)
        feedIngCursor.use {
            while (it.moveToNext()) {
                val o = JSONObject().apply {
                    put("id", it.getString(it.getColumnIndexOrThrow("id")))
                    put("name", it.getString(it.getColumnIndexOrThrow("name")))
                    put("category", it.getString(it.getColumnIndexOrThrow("category")))
                    val eIdx = it.getColumnIndex("energy_kcal_kg")
                    if (eIdx != -1 && !it.isNull(eIdx)) put("energyKcalKg", it.getDouble(eIdx))
                    val cpIdx = it.getColumnIndex("crude_protein")
                    if (cpIdx != -1 && !it.isNull(cpIdx)) put("crudeProtein", it.getDouble(cpIdx))
                    val caIdx = it.getColumnIndex("calcium")
                    if (caIdx != -1 && !it.isNull(caIdx)) put("calcium", it.getDouble(caIdx))
                    val pIdx = it.getColumnIndex("available_phosphorus")
                    if (pIdx != -1 && !it.isNull(pIdx)) put("availablePhosphorus", it.getDouble(pIdx))
                    val mIdx = it.getColumnIndex("methionine")
                    if (mIdx != -1 && !it.isNull(mIdx)) put("methionine", it.getDouble(mIdx))
                    val lIdx = it.getColumnIndex("lysine")
                    if (lIdx != -1 && !it.isNull(lIdx)) put("lysine", it.getDouble(lIdx))
                    val naIdx = it.getColumnIndex("sodium")
                    if (naIdx != -1 && !it.isNull(naIdx)) put("sodium", it.getDouble(naIdx))
                    val cfIdx = it.getColumnIndex("crude_fiber")
                    if (cfIdx != -1 && !it.isNull(cfIdx)) put("crudeFiber", it.getDouble(cfIdx))
                    val fatIdx = it.getColumnIndex("fat")
                    if (fatIdx != -1 && !it.isNull(fatIdx)) put("fat", it.getDouble(fatIdx))
                    val ppkIdx = it.getColumnIndex("price_per_kg")
                    if (ppkIdx != -1 && !it.isNull(ppkIdx)) put("pricePerKg", it.getDouble(ppkIdx))
                    val descIdx = it.getColumnIndex("description")
                    if (descIdx != -1 && !it.isNull(descIdx)) put("description", it.getString(descIdx))
                    val caMillisIdx = it.getColumnIndex("created_at_millis")
                    if (caMillisIdx != -1 && !it.isNull(caMillisIdx)) put("createdAtMillis", it.getLong(caMillisIdx))
                }
                feedIngArr.put(o)
            }
        }
        root.put("customFeedIngredients", feedIngArr)

        // 15. Biosecurity Checks
        val bioArr = JSONArray()
        val bioCursor = db.rawQuery("SELECT * FROM biosecurity_checks", null)
        bioCursor.use {
            while (it.moveToNext()) {
                val o = JSONObject().apply {
                    put("id", it.getString(it.getColumnIndexOrThrow("id")))
                    put("season", it.getString(it.getColumnIndexOrThrow("season")))
                    put("title", it.getString(it.getColumnIndexOrThrow("title")))
                    put("description", it.getString(it.getColumnIndexOrThrow("description")))
                    put("priority", it.getString(it.getColumnIndexOrThrow("priority")))
                    put("isCompleted", it.getInt(it.getColumnIndexOrThrow("is_completed")) == 1)
                    val cDate = it.getColumnIndex("completed_date_jalali_str")
                    if (cDate != -1 && !it.isNull(cDate)) put("completedDateJalaliStr", it.getString(cDate))
                }
                bioArr.put(o)
            }
        }
        root.put("biosecurityChecks", bioArr)

        // 16. Flock Bird Sales
        val salesArr = JSONArray()
        val salesCursor = db.rawQuery("SELECT * FROM flock_bird_sales ORDER BY id ASC", null)
        salesCursor.use {
            while (it.moveToNext()) {
                val o = JSONObject().apply {
                    put("id", it.getLong(it.getColumnIndexOrThrow("id")))
                    put("flockId", it.getLong(it.getColumnIndexOrThrow("flock_id")))
                    put("flockName", it.getString(it.getColumnIndexOrThrow("flock_name")))
                    put("birdCount", it.getInt(it.getColumnIndexOrThrow("bird_count")))
                    put("birdAgeDays", it.getInt(it.getColumnIndexOrThrow("bird_age_days")))
                    val lwIdx = it.getColumnIndex("live_weight_kg")
                    if (lwIdx != -1 && !it.isNull(lwIdx)) put("liveWeightKg", it.getDouble(lwIdx))
                    val upIdx = it.getColumnIndex("unit_price_tomans")
                    if (upIdx != -1 && !it.isNull(upIdx)) put("unitPriceTomans", it.getDouble(upIdx))
                    put("totalRevenueTomans", it.getDouble(it.getColumnIndexOrThrow("total_revenue_tomans")))
                    val bnIdx = it.getColumnIndex("buyer_name")
                    if (bnIdx != -1 && !it.isNull(bnIdx)) put("buyerName", it.getString(bnIdx))
                    val ltIdx = it.getColumnIndex("linked_transaction_id")
                    if (ltIdx != -1 && !it.isNull(ltIdx)) put("linkedTransactionId", it.getLong(ltIdx))
                    put("dateJalaliStr", it.getString(it.getColumnIndexOrThrow("date_jalali_str")))
                    put("timestampMillis", it.getLong(it.getColumnIndexOrThrow("timestamp_millis")))
                    val nIdx = it.getColumnIndex("notes")
                    if (nIdx != -1 && !it.isNull(nIdx)) put("notes", it.getString(nIdx))
                }
                salesArr.put(o)
            }
        }
        root.put("flockBirdSales", salesArr)

        // 17. Flock Bird Slaughters
        val slaughterArr = JSONArray()
        val slaughterCursor = db.rawQuery("SELECT * FROM flock_bird_slaughters ORDER BY id ASC", null)
        slaughterCursor.use {
            while (it.moveToNext()) {
                val o = JSONObject().apply {
                    put("id", it.getLong(it.getColumnIndexOrThrow("id")))
                    put("flockId", it.getLong(it.getColumnIndexOrThrow("flock_id")))
                    put("flockName", it.getString(it.getColumnIndexOrThrow("flock_name")))
                    put("birdCount", it.getInt(it.getColumnIndexOrThrow("bird_count")))
                    put("birdAgeDays", it.getInt(it.getColumnIndexOrThrow("bird_age_days")))
                    val lwIdx = it.getColumnIndex("live_weight_kg")
                    if (lwIdx != -1 && !it.isNull(lwIdx)) put("liveWeightKg", it.getDouble(lwIdx))
                    val cwIdx = it.getColumnIndex("carcass_weight_kg")
                    if (cwIdx != -1 && !it.isNull(cwIdx)) put("carcassWeightKg", it.getDouble(cwIdx))
                    put("dateJalaliStr", it.getString(it.getColumnIndexOrThrow("date_jalali_str")))
                    put("timestampMillis", it.getLong(it.getColumnIndexOrThrow("timestamp_millis")))
                    val nIdx = it.getColumnIndex("notes")
                    if (nIdx != -1 && !it.isNull(nIdx)) put("notes", it.getString(nIdx))
                }
                slaughterArr.put(o)
            }
        }
        root.put("flockBirdSlaughters", slaughterArr)

        return root.toString(2)
    }

    // ==================== MULTI-DEVICE CLOUD MERGE (FEATURE 12) ====================

    fun mergeDatabaseFromJson(jsonStr: String): Boolean {
        return try {
            val root = JSONObject(jsonStr)
            val db = writableDatabase
            db.beginTransaction()
            try {
                // 1. Flocks - merge by ID or name
                if (root.has("flocks")) {
                    val flocksArr = root.getJSONArray("flocks")
                    for (i in 0 until flocksArr.length()) {
                        val fo = flocksArr.getJSONObject(i)
                        val incomingCreated = fo.optLong("createdAtMillis", System.currentTimeMillis())
                        val incomingId = fo.optLong("id", -1L)
                        val cv = ContentValues().apply {
                            if (incomingId > 0) put("id", incomingId)
                            put("name", fo.getString("name"))
                            put("species", fo.getString("species"))
                            put("breed", fo.getString("breed"))
                            put("birth_epoch_millis", fo.getLong("birthEpochMillis"))
                            put("bird_count", fo.getInt("birdCount"))
                            if (!fo.isNull("photoUri")) put("photo_uri", fo.getString("photoUri"))
                            if (!fo.isNull("notes")) put("notes", fo.getString("notes"))
                            put("created_at_millis", incomingCreated)
                            if (fo.has("subgroupsJson") && !fo.isNull("subgroupsJson")) {
                                put("subgroups_json", fo.getString("subgroupsJson"))
                            }
                            put("is_feed_enabled", if (fo.optBoolean("isFeedEnabled", true)) 1 else 0)
                            put("is_water_enabled", if (fo.optBoolean("isWaterEnabled", true)) 1 else 0)
                            if (fo.has("customTempCelsius") && !fo.isNull("customTempCelsius")) {
                                put("custom_temp_celsius", fo.getDouble("customTempCelsius"))
                            }
                        }
                        db.insertWithOnConflict("flocks", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                    }
                }

                // 2. Warehouse Items
                if (root.has("warehouseItems")) {
                    val arr = root.getJSONArray("warehouseItems")
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val cv = ContentValues().apply {
                            if (o.has("id") && o.getLong("id") > 0) put("id", o.getLong("id"))
                            put("name", o.getString("name"))
                            put("category", o.getString("category"))
                            put("current_stock", o.getDouble("currentStock"))
                            put("unit", o.getString("unit"))
                            if (o.has("assignedFlockIdsCsv") && !o.isNull("assignedFlockIdsCsv")) {
                                put("assigned_flock_ids_csv", o.getString("assignedFlockIdsCsv"))
                            }
                            put("min_alert_threshold", o.optDouble("minAlertThreshold", 50.0))
                            put("price_per_unit", o.optDouble("pricePerUnit", 0.0))
                            put("is_for_sale", if (o.optBoolean("isForSale", false)) 1 else 0)
                            if (o.has("notes") && !o.isNull("notes")) put("notes", o.getString("notes"))
                            put("updated_at_millis", o.optLong("updatedAtMillis", System.currentTimeMillis()))
                        }
                        db.insertWithOnConflict("warehouse_items", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                    }
                }

                // 3. Farm Contacts
                if (root.has("farmContacts")) {
                    val arr = root.getJSONArray("farmContacts")
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val cv = ContentValues().apply {
                            if (o.has("id") && o.getLong("id") > 0) put("id", o.getLong("id"))
                            put("full_name", o.getString("fullName"))
                            if (o.has("businessName") && !o.isNull("businessName")) put("business_name", o.getString("businessName"))
                            put("category", o.getString("category"))
                            put("phone_primary", o.getString("phonePrimary"))
                            if (o.has("phoneSecondary") && !o.isNull("phoneSecondary")) put("phone_secondary", o.getString("phoneSecondary"))
                            if (o.has("address") && !o.isNull("address")) put("address", o.getString("address"))
                            if (o.has("provinceCity") && !o.isNull("provinceCity")) put("province_city", o.getString("provinceCity"))
                            put("balance_tomans", o.optDouble("balanceTomans", 0.0))
                            if (o.has("paymentTermsNotes") && !o.isNull("paymentTermsNotes")) put("payment_terms_notes", o.getString("paymentTermsNotes"))
                            put("rating_stars", o.optInt("ratingStars", 5))
                            put("created_at_millis", o.optLong("createdAtMillis", System.currentTimeMillis()))
                        }
                        db.insertWithOnConflict("farm_contacts", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                    }
                }

                // 4. Egg Productions - aggregate without duplicates
                if (root.has("eggProductions")) {
                    val arr = root.getJSONArray("eggProductions")
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val ts = o.getLong("timestampMillis")
                        val fId = o.getLong("flockId")
                        val exists = db.compileStatement("SELECT COUNT(*) FROM egg_productions WHERE flock_id = $fId AND timestamp_millis = $ts").simpleQueryForLong() > 0
                        if (!exists) {
                            val cv = ContentValues().apply {
                                put("flock_id", fId)
                                put("flock_name", o.getString("flockName"))
                                put("healthy_count", o.getInt("healthyCount"))
                                put("broken_count", o.optInt("brokenCount", 0))
                                put("dirty_count", o.optInt("dirtyCount", 0))
                                put("double_yolk_count", o.optInt("doubleYolkCount", 0))
                                put("total_count", o.getInt("totalCount"))
                                put("laying_rate_percent", o.optDouble("layingRatePercent", 0.0))
                                put("date_jalali_str", o.getString("dateJalaliStr"))
                                put("timestamp_millis", ts)
                                put("fertile_count", o.optInt("fertileCount", 0))
                                put("table_egg_count", o.optInt("tableEggCount", 0))
                                put("tray_count", o.optDouble("trayCount", 0.0))
                                if (o.has("trayGrossWeightGrams") && !o.isNull("trayGrossWeightGrams")) {
                                    put("tray_gross_weight_grams", o.getDouble("trayGrossWeightGrams"))
                                }
                                if (o.has("netEggAverageWeightGrams") && !o.isNull("netEggAverageWeightGrams")) {
                                    put("net_egg_average_weight_grams", o.getDouble("netEggAverageWeightGrams"))
                                }
                                if (o.has("notes") && !o.isNull("notes")) put("notes", o.getString("notes"))
                            }
                            db.insert("egg_productions", null, cv)
                        }
                    }
                }

                // 5. Financial Transactions - aggregate without duplicates
                if (root.has("financialTransactions")) {
                    val arr = root.getJSONArray("financialTransactions")
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val ts = o.getLong("timestampMillis")
                        val title = o.getString("title")
                        val exists = db.compileStatement("SELECT COUNT(*) FROM financial_transactions WHERE timestamp_millis = $ts").simpleQueryForLong() > 0
                        if (!exists) {
                            val cv = ContentValues().apply {
                                put("title", title)
                                put("amount_tomans", o.getDouble("amountTomans"))
                                put("type", o.getString("type"))
                                put("category", o.getString("category"))
                                put("date_jalali_str", o.getString("dateJalaliStr"))
                                put("timestamp_millis", ts)
                                if (o.has("relatedFlockId") && !o.isNull("relatedFlockId")) put("related_flock_id", o.getLong("relatedFlockId"))
                                if (o.has("relatedFlockName") && !o.isNull("relatedFlockName")) put("related_flock_name", o.getString("relatedFlockName"))
                                if (o.has("relatedWarehouseItemId") && !o.isNull("relatedWarehouseItemId")) put("related_warehouse_item_id", o.getLong("relatedWarehouseItemId"))
                                put("quantity", o.optDouble("quantity", 0.0))
                                put("unit", o.optString("unit", ""))
                                put("unit_price_tomans", o.optDouble("unitPriceTomans", 0.0))
                                if (o.has("notes") && !o.isNull("notes")) put("notes", o.getString("notes"))
                            }
                            db.insert("financial_transactions", null, cv)
                        }
                    }
                }

                // 6. Mortality Logs - aggregate without duplicates (route legacy LIVE_SALE to flock_bird_sales)
                if (root.has("flockMortalityLogs")) {
                    val arr = root.getJSONArray("flockMortalityLogs")
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val ts = o.getLong("timestampMillis")
                        val fId = o.getLong("flockId")
                        val reasonStr = o.optString("reason", "DISEASE")
                        if (reasonStr.equals("LIVE_SALE", ignoreCase = true)) {
                            val saleExists = db.rawQuery(
                                "SELECT COUNT(*) FROM flock_bird_sales WHERE flock_id = ? AND timestamp_millis = ?",
                                arrayOf(fId.toString(), ts.toString())
                            ).use { c -> c.moveToFirst() && c.getLong(0) > 0L }
                            if (!saleExists) {
                                val cv = ContentValues().apply {
                                    put("flock_id", fId)
                                    put("flock_name", o.getString("flockName"))
                                    put("bird_count", o.getInt("count"))
                                    put("bird_age_days", 1)
                                    put("total_revenue_tomans", 0.0)
                                    put("date_jalali_str", o.getString("dateJalaliStr"))
                                    put("timestamp_millis", ts)
                                    val noteText = o.optString("notes", "").ifBlank {
                                        o.optString("diagnosisHint", "").ifBlank { "منتقل‌شده از بکاپ فروش زنده" }
                                    }
                                    put("notes", noteText)
                                }
                                db.insert("flock_bird_sales", null, cv)
                            }
                        } else {
                            val exists = db.compileStatement("SELECT COUNT(*) FROM flock_mortality_logs WHERE flock_id = $fId AND timestamp_millis = $ts").simpleQueryForLong() > 0
                            if (!exists) {
                                val cv = ContentValues().apply {
                                    put("flock_id", fId)
                                    put("flock_name", o.getString("flockName"))
                                    put("count", o.getInt("count"))
                                    put("reason", reasonStr)
                                    if (o.has("diagnosisHint") && !o.isNull("diagnosisHint")) put("diagnosis_hint", o.getString("diagnosisHint"))
                                    put("date_jalali_str", o.getString("dateJalaliStr"))
                                    put("timestamp_millis", ts)
                                    if (o.has("notes") && !o.isNull("notes")) put("notes", o.getString("notes"))
                                }
                                db.insert("flock_mortality_logs", null, cv)
                            }
                        }
                    }
                }

                // 6b. Bird Sales - aggregate without duplicates
                if (root.has("flockBirdSales")) {
                    val arr = root.getJSONArray("flockBirdSales")
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val ts = o.getLong("timestampMillis")
                        val fId = o.getLong("flockId")
                        val exists = db.rawQuery(
                            "SELECT COUNT(*) FROM flock_bird_sales WHERE flock_id = ? AND timestamp_millis = ?",
                            arrayOf(fId.toString(), ts.toString())
                        ).use { c -> c.moveToFirst() && c.getLong(0) > 0L }
                        if (!exists) {
                            val cv = ContentValues().apply {
                                put("flock_id", fId)
                                put("flock_name", o.getString("flockName"))
                                put("bird_count", o.getInt("birdCount"))
                                put("bird_age_days", o.optInt("birdAgeDays", 1))
                                if (o.has("liveWeightKg") && !o.isNull("liveWeightKg")) put("live_weight_kg", o.getDouble("liveWeightKg"))
                                if (o.has("unitPriceTomans") && !o.isNull("unitPriceTomans")) put("unit_price_tomans", o.getDouble("unitPriceTomans"))
                                put("total_revenue_tomans", o.optDouble("totalRevenueTomans", 0.0))
                                if (o.has("buyerName") && !o.isNull("buyerName")) put("buyer_name", o.getString("buyerName"))
                                if (o.has("linkedTransactionId") && !o.isNull("linkedTransactionId")) put("linked_transaction_id", o.getLong("linkedTransactionId"))
                                put("date_jalali_str", o.getString("dateJalaliStr"))
                                put("timestamp_millis", ts)
                                if (o.has("notes") && !o.isNull("notes")) put("notes", o.getString("notes"))
                            }
                            db.insert("flock_bird_sales", null, cv)
                        }
                    }
                }

                // 6c. Bird Slaughters - aggregate without duplicates
                if (root.has("flockBirdSlaughters")) {
                    val arr = root.getJSONArray("flockBirdSlaughters")
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val ts = o.getLong("timestampMillis")
                        val fId = o.getLong("flockId")
                        val exists = db.rawQuery(
                            "SELECT COUNT(*) FROM flock_bird_slaughters WHERE flock_id = ? AND timestamp_millis = ?",
                            arrayOf(fId.toString(), ts.toString())
                        ).use { c -> c.moveToFirst() && c.getLong(0) > 0L }
                        if (!exists) {
                            val cv = ContentValues().apply {
                                put("flock_id", fId)
                                put("flock_name", o.getString("flockName"))
                                put("bird_count", o.getInt("birdCount"))
                                put("bird_age_days", o.optInt("birdAgeDays", 1))
                                if (o.has("liveWeightKg") && !o.isNull("liveWeightKg")) put("live_weight_kg", o.getDouble("liveWeightKg"))
                                if (o.has("carcassWeightKg") && !o.isNull("carcassWeightKg")) put("carcass_weight_kg", o.getDouble("carcassWeightKg"))
                                put("date_jalali_str", o.getString("dateJalaliStr"))
                                put("timestamp_millis", ts)
                                if (o.has("notes") && !o.isNull("notes")) put("notes", o.getString("notes"))
                            }
                            db.insert("flock_bird_slaughters", null, cv)
                        }
                    }
                }

                // 15. Flock Observations
                if (root.has("flockObservations")) {
                    val arr = root.getJSONArray("flockObservations")
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val cv = ContentValues().apply {
                            if (o.has("id") && o.getLong("id") > 0) put("id", o.getLong("id"))
                            put("flock_id", o.getLong("flockId"))
                            put("flock_name", o.getString("flockName"))
                            put("note", o.getString("note"))
                            put("author_role", o.optString("authorRole", "MANAGER"))
                            put("category", o.optString("category", "OBSERVATION"))
                            put("date_jalali_str", o.getString("dateJalaliStr"))
                            put("timestamp_millis", o.getLong("timestampMillis"))
                        }
                        db.insertWithOnConflict("flock_observations", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                    }
                }

                // 16. Custom Feed Ingredients
                if (root.has("customFeedIngredients")) {
                    val arr = root.getJSONArray("customFeedIngredients")
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val cv = ContentValues().apply {
                            put("id", o.getString("id"))
                            put("name", o.getString("name"))
                            put("category", o.getString("category"))
                            if (o.has("energyKcalKg") && !o.isNull("energyKcalKg")) put("energy_kcal_kg", o.getDouble("energyKcalKg"))
                            if (o.has("crudeProtein") && !o.isNull("crudeProtein")) put("crude_protein", o.getDouble("crudeProtein"))
                            if (o.has("calcium") && !o.isNull("calcium")) put("calcium", o.getDouble("calcium"))
                            if (o.has("availablePhosphorus") && !o.isNull("availablePhosphorus")) put("available_phosphorus", o.getDouble("availablePhosphorus"))
                            if (o.has("methionine") && !o.isNull("methionine")) put("methionine", o.getDouble("methionine"))
                            if (o.has("lysine") && !o.isNull("lysine")) put("lysine", o.getDouble("lysine"))
                            if (o.has("sodium") && !o.isNull("sodium")) put("sodium", o.getDouble("sodium"))
                            if (o.has("crudeFiber") && !o.isNull("crudeFiber")) put("crude_fiber", o.getDouble("crudeFiber"))
                            if (o.has("fat") && !o.isNull("fat")) put("fat", o.getDouble("fat"))
                            if (o.has("pricePerKg") && !o.isNull("pricePerKg")) put("price_per_kg", o.getDouble("pricePerKg"))
                            if (o.has("description") && !o.isNull("description")) put("description", o.getString("description"))
                            if (o.has("createdAtMillis") && !o.isNull("createdAtMillis")) put("created_at_millis", o.getLong("createdAtMillis"))
                        }
                        db.insertWithOnConflict("custom_feed_ingredients", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                    }
                }

                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            notifyDataChanged()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ==================== DATABASE IMPORT FROM JSON ====================

    fun importDatabaseFromJson(jsonStr: String): Boolean {
        return try {
            val root = JSONObject(jsonStr)
            val db = writableDatabase
            db.beginTransaction()
            try {
                // 1. Flocks (Preserving original IDs)
                if (root.has("flocks")) {
                    val flocksArr = root.getJSONArray("flocks")
                    for (i in 0 until flocksArr.length()) {
                        val fo = flocksArr.getJSONObject(i)
                        val cv = ContentValues().apply {
                            if (fo.has("id") && fo.getLong("id") > 0) {
                                put("id", fo.getLong("id"))
                            }
                            put("name", fo.getString("name"))
                            put("species", fo.getString("species"))
                            put("breed", fo.getString("breed"))
                            put("birth_epoch_millis", fo.getLong("birthEpochMillis"))
                            put("bird_count", fo.getInt("birdCount"))
                            if (!fo.isNull("photoUri")) put("photo_uri", fo.getString("photoUri"))
                            if (!fo.isNull("notes")) put("notes", fo.getString("notes"))
                            put("created_at_millis", fo.optLong("createdAtMillis", System.currentTimeMillis()))
                            if (fo.has("subgroupsJson") && !fo.isNull("subgroupsJson")) {
                                put("subgroups_json", fo.getString("subgroupsJson"))
                            }
                            put("is_feed_enabled", if (fo.optBoolean("isFeedEnabled", true)) 1 else 0)
                            put("is_water_enabled", if (fo.optBoolean("isWaterEnabled", true)) 1 else 0)
                            if (fo.has("customTempCelsius") && !fo.isNull("customTempCelsius")) {
                                put("custom_temp_celsius", fo.getDouble("customTempCelsius"))
                            }
                        }
                        db.insertWithOnConflict("flocks", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                    }
                }

                // 2. Custom Breeds
                if (root.has("customBreeds")) {
                    val cbArr = root.getJSONArray("customBreeds")
                    for (i in 0 until cbArr.length()) {
                        val cbo = cbArr.getJSONObject(i)
                        val cv = ContentValues().apply {
                            if (cbo.has("id") && cbo.getLong("id") > 0) {
                                put("id", cbo.getLong("id"))
                            }
                            put("name", cbo.getString("name"))
                            put("species", cbo.getString("species"))
                            put("production_type", cbo.getString("productionType"))
                            put("adult_feed_grams", cbo.getDouble("adultFeedGrams"))
                            put("adult_water_ml", cbo.getDouble("adultWaterMl"))
                            put("weekly_rates_json", cbo.optString("weeklyRatesJson", "[]"))
                            if (!cbo.isNull("notes")) put("notes", cbo.getString("notes"))
                        }
                        db.insertWithOnConflict("custom_breeds", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                    }
                }

                // 3. Calendar Schedules
                if (root.has("schedules")) {
                    val schedArr = root.getJSONArray("schedules")
                    for (i in 0 until schedArr.length()) {
                        val so = schedArr.getJSONObject(i)
                        val fid = so.optLong("flockId", -1L)
                        val cv = ContentValues().apply {
                            if (so.has("id") && so.getLong("id") > 0) {
                                put("id", so.getLong("id"))
                            }
                            if (fid > 0) put("flock_id", fid)
                            if (!so.isNull("flockName")) put("flock_name", so.getString("flockName"))
                            put("schedule_type", so.getString("scheduleType"))
                            put("title", so.getString("title"))
                            if (!so.isNull("notes")) put("notes", so.getString("notes"))
                            put("days_of_week_csv", so.optString("daysOfWeek", "0,1,2,3,4,5,6"))
                            put("hour", so.getInt("hour"))
                            put("minute", so.getInt("minute"))
                            put("is_enabled", if (so.optBoolean("isEnabled", true)) 1 else 0)
                            put("schedule_mode", so.optString("scheduleMode", "WEEKLY"))
                            if (so.has("dayOfMonth") && !so.isNull("dayOfMonth")) {
                                put("day_of_month", so.getInt("dayOfMonth"))
                            }
                        }
                        db.insertWithOnConflict("calendar_schedules", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                    }
                }

                // 4. Warehouse Items (With Assigned Flock IDs)
                if (root.has("warehouseItems")) {
                    val whArr = root.getJSONArray("warehouseItems")
                    for (i in 0 until whArr.length()) {
                        val wo = whArr.getJSONObject(i)
                        val cv = ContentValues().apply {
                            if (wo.has("id") && wo.getLong("id") > 0) {
                                put("id", wo.getLong("id"))
                            }
                            put("name", wo.getString("name"))
                            put("category", wo.getString("category"))
                            put("current_stock", wo.getDouble("currentStock"))
                            put("unit", wo.optString("unit", "کیلوگرم"))
                            put("assigned_flock_ids_csv", wo.optString("assignedFlockIds", ""))
                            put("price_per_unit", wo.optDouble("pricePerUnit", 0.0))
                            put("min_alert_threshold", wo.optDouble("minAlertThreshold", 50.0))
                            put("is_for_sale", if (wo.optBoolean("isForSale", false)) 1 else 0)
                            if (!wo.isNull("notes")) put("notes", wo.getString("notes"))
                            put("updated_at_millis", wo.optLong("updatedAtMillis", System.currentTimeMillis()))
                        }
                        db.insertWithOnConflict("warehouse_items", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                    }
                }

                // 5. Feed & Water Charges (Live Monitoring)
                if (root.has("feedWaterCharges")) {
                    val chargesArr = root.getJSONArray("feedWaterCharges")
                    for (i in 0 until chargesArr.length()) {
                        val co = chargesArr.getJSONObject(i)
                        val cv = ContentValues().apply {
                            if (co.has("id") && co.getLong("id") > 0) {
                                put("id", co.getLong("id"))
                            }
                            put("flock_id", co.getLong("flockId"))
                            put("item_type", co.getString("itemType"))
                            put("charged_amount", co.getDouble("chargedAmount"))
                            put("charge_timestamp_millis", co.getLong("chargeTimestampMillis"))
                            put("date_jalali_str", co.getString("dateJalaliStr"))
                            if (co.has("notes") && !co.isNull("notes")) {
                                put("notes", co.getString("notes"))
                            }
                        }
                        db.insertWithOnConflict("feed_water_charges", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                    }
                }

                // 6. Farm & App Settings
                if (root.has("appSettings")) {
                    val settingsObj = root.getJSONObject("appSettings")
                    val keys = settingsObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val value = settingsObj.getString(key)
                        val cv = ContentValues().apply {
                            put("setting_key", key)
                            put("setting_value", value)
                        }
                        db.insertWithOnConflict("app_settings", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                    }
                }

                // 7. Egg Productions
                if (root.has("eggProductions")) {
                    val arr = root.getJSONArray("eggProductions")
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val cv = ContentValues().apply {
                            if (o.has("id") && o.getLong("id") > 0) put("id", o.getLong("id"))
                            put("flock_id", o.getLong("flockId"))
                            put("flock_name", o.getString("flockName"))
                            put("healthy_count", o.getInt("healthyCount"))
                            put("broken_count", o.getInt("brokenCount"))
                            put("dirty_count", o.getInt("dirtyCount"))
                            put("double_yolk_count", o.getInt("doubleYolkCount"))
                            put("total_count", o.getInt("totalCount"))
                            put("laying_rate_percent", o.getDouble("layingRatePercent"))
                            put("date_jalali_str", o.getString("dateJalaliStr"))
                            put("timestamp_millis", o.getLong("timestampMillis"))
                            if (o.has("notes") && !o.isNull("notes")) put("notes", o.getString("notes"))
                        }
                        db.insertWithOnConflict("egg_productions", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                    }
                }

                // 8. Financial Transactions
                if (root.has("financialTransactions")) {
                    val arr = root.getJSONArray("financialTransactions")
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val cv = ContentValues().apply {
                            if (o.has("id") && o.getLong("id") > 0) put("id", o.getLong("id"))
                            put("title", o.getString("title"))
                            put("amount_tomans", o.getDouble("amountTomans"))
                            put("type", o.getString("type"))
                            put("category", o.getString("category"))
                            put("date_jalali_str", o.getString("dateJalaliStr"))
                            put("timestamp_millis", o.getLong("timestampMillis"))
                            if (o.has("relatedFlockId") && !o.isNull("relatedFlockId")) put("related_flock_id", o.getLong("relatedFlockId"))
                            if (o.has("relatedFlockName") && !o.isNull("relatedFlockName")) put("related_flock_name", o.getString("relatedFlockName"))
                            if (o.has("relatedWarehouseItemId") && !o.isNull("relatedWarehouseItemId")) put("related_warehouse_item_id", o.getLong("relatedWarehouseItemId"))
                            put("quantity", o.getDouble("quantity"))
                            put("unit", o.getString("unit"))
                            put("unit_price_tomans", o.getDouble("unitPriceTomans"))
                            if (o.has("notes") && !o.isNull("notes")) put("notes", o.getString("notes"))
                        }
                        db.insertWithOnConflict("financial_transactions", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                    }
                }

                // 9. Bird Transfers
                if (root.has("birdTransfers")) {
                    val arr = root.getJSONArray("birdTransfers")
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val cv = ContentValues().apply {
                            if (o.has("id") && o.getLong("id") > 0) put("id", o.getLong("id"))
                            put("from_flock_id", o.getLong("fromFlockId"))
                            put("from_flock_name", o.getString("fromFlockName"))
                            put("to_flock_id", o.getLong("toFlockId"))
                            put("to_flock_name", o.getString("toFlockName"))
                            put("bird_count", o.getInt("birdCount"))
                            put("date_jalali_str", o.getString("dateJalaliStr"))
                            put("timestamp_millis", o.getLong("timestampMillis"))
                            if (o.has("notes") && !o.isNull("notes")) put("notes", o.getString("notes"))
                        }
                        db.insertWithOnConflict("bird_transfers", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                    }
                }

                // 10. Ration Plans
                if (root.has("rationPlans")) {
                    val arr = root.getJSONArray("rationPlans")
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val cv = ContentValues().apply {
                            if (o.has("id") && o.getLong("id") > 0) put("id", o.getLong("id"))
                            put("name", o.getString("name"))
                            put("item_type", o.getString("itemType"))
                            put("target_bird", o.getString("targetBird"))
                            put("target_stage", o.getString("targetStage"))
                            put("pricing_mode", o.getString("pricingMode"))
                            put("manual_price_per_kg", o.getDouble("manualPricePerKg"))
                            put("batch_base_kg", o.getDouble("batchBaseKg"))
                            put("ingredients_json", o.getString("ingredientsJson"))
                            if (o.has("notes") && !o.isNull("notes")) put("notes", o.getString("notes"))
                            put("created_at_millis", o.optLong("createdAtMillis", System.currentTimeMillis()))
                            put("updated_at_millis", o.optLong("updatedAtMillis", System.currentTimeMillis()))
                        }
                        db.insertWithOnConflict("ration_plans", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                    }
                }

                // 11. Incubation Batches (Part 3)
                if (root.has("incubationBatches")) {
                    val arr = root.getJSONArray("incubationBatches")
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val cv = ContentValues().apply {
                            if (o.has("id") && o.getLong("id") > 0) put("id", o.getLong("id"))
                            if (o.has("flockId") && !o.isNull("flockId")) put("flock_id", o.getLong("flockId"))
                            put("batch_name", o.getString("batchName"))
                            put("species", o.getString("species"))
                            put("total_eggs_set", o.getInt("totalEggsSet"))
                            put("start_date_jalali", o.getString("startDateJalali"))
                            put("start_timestamp", o.getLong("startTimestamp"))
                            put("expected_hatch_date_jalali", o.getString("expectedHatchDateJalali"))
                            put("expected_hatch_timestamp", o.getLong("expectedHatchTimestamp"))
                            put("candling1_done", if (o.optBoolean("candling1Done", false)) 1 else 0)
                            if (o.has("fertileEggsCount") && !o.isNull("fertileEggsCount")) put("fertile_eggs_count", o.getInt("fertileEggsCount"))
                            put("candling2_done", if (o.optBoolean("candling2Done", false)) 1 else 0)
                            if (o.has("deadEmbryoCount") && !o.isNull("deadEmbryoCount")) put("dead_embryo_count", o.getInt("deadEmbryoCount"))
                            if (o.has("hatchedChicksCount") && !o.isNull("hatchedChicksCount")) put("hatched_chicks_count", o.getInt("hatchedChicksCount"))
                            put("status", o.optString("status", "SETTER"))
                            if (o.has("notes") && !o.isNull("notes")) put("notes", o.getString("notes"))
                        }
                        db.insertWithOnConflict("incubation_batches", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                    }
                }

                // 12. Farm Contacts (Part 1)
                if (root.has("farmContacts")) {
                    val arr = root.getJSONArray("farmContacts")
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val cv = ContentValues().apply {
                            if (o.has("id") && o.getLong("id") > 0) put("id", o.getLong("id"))
                            put("full_name", o.getString("fullName"))
                            if (o.has("businessName") && !o.isNull("businessName")) put("business_name", o.getString("businessName"))
                            put("category", o.getString("category"))
                            put("phone_primary", o.getString("phonePrimary"))
                            if (o.has("phoneSecondary") && !o.isNull("phoneSecondary")) put("phone_secondary", o.getString("phoneSecondary"))
                            if (o.has("address") && !o.isNull("address")) put("address", o.getString("address"))
                            if (o.has("provinceCity") && !o.isNull("provinceCity")) put("province_city", o.getString("provinceCity"))
                            put("balance_tomans", o.optDouble("balanceTomans", 0.0))
                            if (o.has("paymentTermsNotes") && !o.isNull("paymentTermsNotes")) put("payment_terms_notes", o.getString("paymentTermsNotes"))
                            put("rating_stars", o.optInt("ratingStars", 5))
                            put("created_at_millis", o.optLong("createdAtMillis", System.currentTimeMillis()))
                        }
                        db.insertWithOnConflict("farm_contacts", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                    }
                }

                // 13. Flock Mortality Logs (Part 1 - route legacy LIVE_SALE to flock_bird_sales)
                if (root.has("flockMortalityLogs")) {
                    val arr = root.getJSONArray("flockMortalityLogs")
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val reasonStr = o.optString("reason", "DISEASE")
                        if (reasonStr.equals("LIVE_SALE", ignoreCase = true)) {
                            val cv = ContentValues().apply {
                                put("flock_id", o.getLong("flockId"))
                                put("flock_name", o.getString("flockName"))
                                put("bird_count", o.getInt("count"))
                                put("bird_age_days", 1)
                                put("total_revenue_tomans", 0.0)
                                put("date_jalali_str", o.getString("dateJalaliStr"))
                                put("timestamp_millis", o.getLong("timestampMillis"))
                                val noteText = o.optString("notes", "").ifBlank {
                                    o.optString("diagnosisHint", "").ifBlank { "منتقل‌شده از بکاپ فروش زنده" }
                                }
                                put("notes", noteText)
                            }
                            db.insert("flock_bird_sales", null, cv)
                        } else {
                            val cv = ContentValues().apply {
                                if (o.has("id") && o.getLong("id") > 0) put("id", o.getLong("id"))
                                put("flock_id", o.getLong("flockId"))
                                put("flock_name", o.getString("flockName"))
                                put("count", o.getInt("count"))
                                put("reason", reasonStr)
                                if (o.has("diagnosisHint") && !o.isNull("diagnosisHint")) put("diagnosis_hint", o.getString("diagnosisHint"))
                                put("date_jalali_str", o.getString("dateJalaliStr"))
                                put("timestamp_millis", o.getLong("timestampMillis"))
                                if (o.has("notes") && !o.isNull("notes")) put("notes", o.getString("notes"))
                            }
                            db.insertWithOnConflict("flock_mortality_logs", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                        }
                    }
                }

                // 13b. Flock Bird Sales
                if (root.has("flockBirdSales")) {
                    val arr = root.getJSONArray("flockBirdSales")
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val cv = ContentValues().apply {
                            if (o.has("id") && o.getLong("id") > 0) put("id", o.getLong("id"))
                            put("flock_id", o.getLong("flockId"))
                            put("flock_name", o.getString("flockName"))
                            put("bird_count", o.getInt("birdCount"))
                            put("bird_age_days", o.optInt("birdAgeDays", 1))
                            if (o.has("liveWeightKg") && !o.isNull("liveWeightKg")) put("live_weight_kg", o.getDouble("liveWeightKg"))
                            if (o.has("unitPriceTomans") && !o.isNull("unitPriceTomans")) put("unit_price_tomans", o.getDouble("unitPriceTomans"))
                            put("total_revenue_tomans", o.optDouble("totalRevenueTomans", 0.0))
                            if (o.has("buyerName") && !o.isNull("buyerName")) put("buyer_name", o.getString("buyerName"))
                            if (o.has("linkedTransactionId") && !o.isNull("linkedTransactionId")) put("linked_transaction_id", o.getLong("linkedTransactionId"))
                            put("date_jalali_str", o.getString("dateJalaliStr"))
                            put("timestamp_millis", o.getLong("timestampMillis"))
                            if (o.has("notes") && !o.isNull("notes")) put("notes", o.getString("notes"))
                        }
                        db.insertWithOnConflict("flock_bird_sales", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                    }
                }

                // 13c. Flock Bird Slaughters
                if (root.has("flockBirdSlaughters")) {
                    val arr = root.getJSONArray("flockBirdSlaughters")
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val cv = ContentValues().apply {
                            if (o.has("id") && o.getLong("id") > 0) put("id", o.getLong("id"))
                            put("flock_id", o.getLong("flockId"))
                            put("flock_name", o.getString("flockName"))
                            put("bird_count", o.getInt("birdCount"))
                            put("bird_age_days", o.optInt("birdAgeDays", 1))
                            if (o.has("liveWeightKg") && !o.isNull("liveWeightKg")) put("live_weight_kg", o.getDouble("liveWeightKg"))
                            if (o.has("carcassWeightKg") && !o.isNull("carcassWeightKg")) put("carcass_weight_kg", o.getDouble("carcassWeightKg"))
                            put("date_jalali_str", o.getString("dateJalaliStr"))
                            put("timestamp_millis", o.getLong("timestampMillis"))
                            if (o.has("notes") && !o.isNull("notes")) put("notes", o.getString("notes"))
                        }
                        db.insertWithOnConflict("flock_bird_slaughters", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                    }
                }

                // 14. Biosecurity Checks (Part 2)
                if (root.has("biosecurityChecks")) {
                    val arr = root.getJSONArray("biosecurityChecks")
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val cv = ContentValues().apply {
                            put("id", o.getString("id"))
                            put("season", o.getString("season"))
                            put("title", o.getString("title"))
                            put("description", o.optString("description", "دستورالعمل بیوامنیتی"))
                            put("priority", o.optString("priority", "HIGH"))
                            put("is_completed", if (o.optBoolean("isCompleted", false)) 1 else 0)
                            if (o.has("completedDateJalaliStr") && !o.isNull("completedDateJalaliStr")) {
                                put("completed_date_jalali_str", o.getString("completedDateJalaliStr"))
                            }
                        }
                        db.insertWithOnConflict("biosecurity_checks", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                    }
                } else {
                    ensureDefaultBiosecurityChecks(db)
                }

                // 15. Flock Observations
                if (root.has("flockObservations")) {
                    val arr = root.getJSONArray("flockObservations")
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val cv = ContentValues().apply {
                            if (o.has("id") && o.getLong("id") > 0) put("id", o.getLong("id"))
                            put("flock_id", o.getLong("flockId"))
                            put("flock_name", o.getString("flockName"))
                            put("note", o.getString("note"))
                            put("author_role", o.optString("authorRole", "MANAGER"))
                            put("category", o.optString("category", "OBSERVATION"))
                            put("date_jalali_str", o.getString("dateJalaliStr"))
                            put("timestamp_millis", o.getLong("timestampMillis"))
                        }
                        db.insertWithOnConflict("flock_observations", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                    }
                }

                // 16. Custom Feed Ingredients
                if (root.has("customFeedIngredients")) {
                    val arr = root.getJSONArray("customFeedIngredients")
                    for (i in 0 until arr.length()) {
                        val o = arr.getJSONObject(i)
                        val cv = ContentValues().apply {
                            put("id", o.getString("id"))
                            put("name", o.getString("name"))
                            put("category", o.getString("category"))
                            if (o.has("energyKcalKg") && !o.isNull("energyKcalKg")) put("energy_kcal_kg", o.getDouble("energyKcalKg"))
                            if (o.has("crudeProtein") && !o.isNull("crudeProtein")) put("crude_protein", o.getDouble("crudeProtein"))
                            if (o.has("calcium") && !o.isNull("calcium")) put("calcium", o.getDouble("calcium"))
                            if (o.has("availablePhosphorus") && !o.isNull("availablePhosphorus")) put("available_phosphorus", o.getDouble("availablePhosphorus"))
                            if (o.has("methionine") && !o.isNull("methionine")) put("methionine", o.getDouble("methionine"))
                            if (o.has("lysine") && !o.isNull("lysine")) put("lysine", o.getDouble("lysine"))
                            if (o.has("sodium") && !o.isNull("sodium")) put("sodium", o.getDouble("sodium"))
                            if (o.has("crudeFiber") && !o.isNull("crudeFiber")) put("crude_fiber", o.getDouble("crudeFiber"))
                            if (o.has("fat") && !o.isNull("fat")) put("fat", o.getDouble("fat"))
                            if (o.has("pricePerKg") && !o.isNull("pricePerKg")) put("price_per_kg", o.getDouble("pricePerKg"))
                            if (o.has("description") && !o.isNull("description")) put("description", o.getString("description"))
                            if (o.has("createdAtMillis") && !o.isNull("createdAtMillis")) put("created_at_millis", o.getLong("createdAtMillis"))
                        }
                        db.insertWithOnConflict("custom_feed_ingredients", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
                    }
                }

                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            notifyDataChanged()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun seedDatabaseFromAssetsIfEmpty(context: Context) {
        val prefs = context.getSharedPreferences("poultry_db_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("is_db_seeded_v1", false)) {
            return
        }
        try {
            val db = writableDatabase
            val flockCount = db.compileStatement("SELECT COUNT(*) FROM flocks").simpleQueryForLong()
            val settingsCount = db.compileStatement("SELECT COUNT(*) FROM app_settings").simpleQueryForLong()
            if (flockCount == 0L && settingsCount == 0L) {
                // Populate default app settings on first launch only without deleting anything
                val jsonStr = context.assets.open("default_database.json").bufferedReader().use { it.readText() }
                val root = JSONObject(jsonStr)
                if (root.has("appSettings")) {
                    val settingsObj = root.getJSONObject("appSettings")
                    val keys = settingsObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val value = settingsObj.getString(key)
                        val cv = ContentValues().apply {
                            put("setting_key", key)
                            put("setting_value", value)
                        }
                        db.insertWithOnConflict("app_settings", null, cv, SQLiteDatabase.CONFLICT_IGNORE)
                    }
                }
            }
            prefs.edit().putBoolean("is_db_seeded_v1", true).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun reloadDatabaseFromAssets(context: Context): Boolean {
        return try {
            val jsonStr = context.assets.open("default_database.json").bufferedReader().use { it.readText() }
            importDatabaseFromJson(jsonStr)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ==================== RATION PLANS (جیره‌ساز) ====================

    fun serializeRationIngredients(ingredients: List<RationIngredient>): String {
        val arr = JSONArray()
        for (item in ingredients) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("percentageOrAmount", item.percentageOrAmount)
                put("pricePerKg", item.pricePerKg)
                if (item.crudeProteinPercent != null) {
                    put("crudeProteinPercent", item.crudeProteinPercent)
                }
                put("category", item.category)
                if (item.energyKcalKg != null) put("energyKcalKg", item.energyKcalKg)
                if (item.calciumPercent != null) put("calciumPercent", item.calciumPercent)
                if (item.availablePhosphorusPercent != null) put("availablePhosphorusPercent", item.availablePhosphorusPercent)
                if (item.methioninePercent != null) put("methioninePercent", item.methioninePercent)
                if (item.lysinePercent != null) put("lysinePercent", item.lysinePercent)
                if (item.sodiumPercent != null) put("sodiumPercent", item.sodiumPercent)
                if (item.crudeFiberPercent != null) put("crudeFiberPercent", item.crudeFiberPercent)
                if (item.fatPercent != null) put("fatPercent", item.fatPercent)
            }
            arr.put(obj)
        }
        return arr.toString()
    }

    fun parseRationIngredients(json: String?): List<RationIngredient> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<RationIngredient>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    RationIngredient(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        name = obj.getString("name"),
                        percentageOrAmount = obj.getDouble("percentageOrAmount"),
                        pricePerKg = obj.optDouble("pricePerKg", 0.0),
                        crudeProteinPercent = if (obj.has("crudeProteinPercent") && !obj.isNull("crudeProteinPercent")) obj.getDouble("crudeProteinPercent") else null,
                        category = obj.optString("category", "غلات"),
                        energyKcalKg = if (obj.has("energyKcalKg") && !obj.isNull("energyKcalKg")) obj.getDouble("energyKcalKg") else null,
                        calciumPercent = if (obj.has("calciumPercent") && !obj.isNull("calciumPercent")) obj.getDouble("calciumPercent") else null,
                        availablePhosphorusPercent = if (obj.has("availablePhosphorusPercent") && !obj.isNull("availablePhosphorusPercent")) obj.getDouble("availablePhosphorusPercent") else null,
                        methioninePercent = if (obj.has("methioninePercent") && !obj.isNull("methioninePercent")) obj.getDouble("methioninePercent") else null,
                        lysinePercent = if (obj.has("lysinePercent") && !obj.isNull("lysinePercent")) obj.getDouble("lysinePercent") else null,
                        sodiumPercent = if (obj.has("sodiumPercent") && !obj.isNull("sodiumPercent")) obj.getDouble("sodiumPercent") else null,
                        crudeFiberPercent = if (obj.has("crudeFiberPercent") && !obj.isNull("crudeFiberPercent")) obj.getDouble("crudeFiberPercent") else null,
                        fatPercent = if (obj.has("fatPercent") && !obj.isNull("fatPercent")) obj.getDouble("fatPercent") else null
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ==================== CUSTOM FEED INGREDIENTS ====================

    fun insertCustomFeedIngredient(item: com.example.poultryapp.data.model.PoultryFeedRawItem): Boolean {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("id", item.id)
            put("name", item.name)
            put("category", item.category.name)
            put("energy_kcal_kg", item.energyKcalKg)
            put("crude_protein", item.crudeProtein)
            put("calcium", item.calcium)
            put("available_phosphorus", item.availablePhosphorus)
            put("methionine", item.methionine)
            put("lysine", item.lysine)
            put("sodium", item.sodium)
            put("crude_fiber", item.crudeFiber)
            put("fat", item.fat)
            put("price_per_kg", item.defaultPricePerKg)
            put("description", item.description)
            put("created_at_millis", System.currentTimeMillis())
        }
        val res = db.insertWithOnConflict("custom_feed_ingredients", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        notifyDataChanged()
        return res != -1L
    }

    fun getAllCustomFeedIngredients(): List<com.example.poultryapp.data.model.PoultryFeedRawItem> {
        val db = readableDatabase
        val list = mutableListOf<com.example.poultryapp.data.model.PoultryFeedRawItem>()
        try {
            val cursor = db.rawQuery("SELECT * FROM custom_feed_ingredients ORDER BY name ASC", null)
            cursor.use {
                while (it.moveToNext()) {
                    val catStr = it.getString(it.getColumnIndexOrThrow("category"))
                    val cat = com.example.poultryapp.data.model.FeedCategory.fromString(catStr)
                    list.add(
                        com.example.poultryapp.data.model.PoultryFeedRawItem(
                            id = it.getString(it.getColumnIndexOrThrow("id")),
                            name = it.getString(it.getColumnIndexOrThrow("name")),
                            category = cat,
                            energyKcalKg = it.getDouble(it.getColumnIndexOrThrow("energy_kcal_kg")),
                            crudeProtein = it.getDouble(it.getColumnIndexOrThrow("crude_protein")),
                            calcium = it.getDouble(it.getColumnIndexOrThrow("calcium")),
                            availablePhosphorus = it.getDouble(it.getColumnIndexOrThrow("available_phosphorus")),
                            methionine = it.getDouble(it.getColumnIndexOrThrow("methionine")),
                            lysine = it.getDouble(it.getColumnIndexOrThrow("lysine")),
                            sodium = it.getDouble(it.getColumnIndexOrThrow("sodium")),
                            crudeFiber = it.getDouble(it.getColumnIndexOrThrow("crude_fiber")),
                            fat = it.getDouble(it.getColumnIndexOrThrow("fat")),
                            defaultPricePerKg = it.getDouble(it.getColumnIndexOrThrow("price_per_kg")),
                            description = it.getString(it.getColumnIndexOrThrow("description")) ?: "",
                            iconRes = com.example.poultryapp.R.drawable.ic_feed_custom,
                            isCustom = true
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun deleteCustomFeedIngredient(id: String): Boolean {
        val db = writableDatabase
        val rows = db.delete("custom_feed_ingredients", "id = ?", arrayOf(id))
        notifyDataChanged()
        return rows > 0
    }

    fun ensureDefaultRationPlans(db: SQLiteDatabase) {
        try {
            val cursor = db.rawQuery("SELECT COUNT(*) FROM ration_plans", null)
            var count = 0
            if (cursor.moveToFirst()) {
                count = cursor.getInt(0)
            }
            cursor.close()
            if (count == 0) {
                insertDefaultRationTemplates(db)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun insertDefaultRationTemplates(db: SQLiteDatabase) {
        val now = System.currentTimeMillis()

        // 1. دان استارتر جوجه گوشتی (۰ تا ۱۰ روزگی)
        val broilerStarterIngredients = listOf(
            RationIngredient(name = "ذرت زرد برزیلی / درجه یک", percentageOrAmount = 55.0, pricePerKg = 12500.0, crudeProteinPercent = 8.5, category = "غلات"),
            RationIngredient(name = "کنجاله سویا ۴۴٪ پروتئین", percentageOrAmount = 35.0, pricePerKg = 22000.0, crudeProteinPercent = 44.0, category = "پروتئینی"),
            RationIngredient(name = "روغن سویا / گیاهی خالص", percentageOrAmount = 2.5, pricePerKg = 58000.0, crudeProteinPercent = 0.0, category = "روغن و چربی"),
            RationIngredient(name = "دی‌کلسیم فسفات (DCP)", percentageOrAmount = 1.8, pricePerKg = 35000.0, crudeProteinPercent = 0.0, category = "معدنی"),
            RationIngredient(name = "کربنات کلسیم / پودر صدف", percentageOrAmount = 1.4, pricePerKg = 4000.0, crudeProteinPercent = 0.0, category = "معدنی"),
            RationIngredient(name = "نمک طعام تصفیه شده", percentageOrAmount = 0.3, pricePerKg = 3000.0, crudeProteinPercent = 0.0, category = "معدنی"),
            RationIngredient(name = "پرمیکس ویتامینه و معدنی ویژه گوشتی", percentageOrAmount = 2.5, pricePerKg = 95000.0, crudeProteinPercent = 0.0, category = "ویتامینه"),
            RationIngredient(name = "اسید آمینه DL-متیونین خالص", percentageOrAmount = 0.3, pricePerKg = 190000.0, crudeProteinPercent = 58.0, category = "اسید آمینه"),
            RationIngredient(name = "اسید آمینه L-لیزین هیدروکلراید", percentageOrAmount = 0.2, pricePerKg = 140000.0, crudeProteinPercent = 94.0, category = "اسید آمینه"),
            RationIngredient(name = "توکسین‌بایندر چند جزئی", percentageOrAmount = 1.0, pricePerKg = 45000.0, crudeProteinPercent = 0.0, category = "دارویی")
        )

        // 2. دان تخم‌گذار بومی و صنعتی (فاز پیک تولید)
        val layerPeakIngredients = listOf(
            RationIngredient(name = "ذرت زرد دانه‌بندی شده", percentageOrAmount = 50.0, pricePerKg = 12500.0, crudeProteinPercent = 8.5, category = "غلات"),
            RationIngredient(name = "کنجاله سویا ۴۴٪", percentageOrAmount = 24.0, pricePerKg = 22000.0, crudeProteinPercent = 44.0, category = "پروتئینی"),
            RationIngredient(name = "سبوس گندم نرم", percentageOrAmount = 10.0, pricePerKg = 8500.0, crudeProteinPercent = 14.5, category = "غلات"),
            RationIngredient(name = "پودر صدف دریایی درشت / کربنات", percentageOrAmount = 8.5, pricePerKg = 4000.0, crudeProteinPercent = 0.0, category = "معدنی"),
            RationIngredient(name = "دی‌کلسیم فسفات", percentageOrAmount = 1.5, pricePerKg = 35000.0, crudeProteinPercent = 0.0, category = "معدنی"),
            RationIngredient(name = "پرمیکس ویتامینه تخم‌گذار ۲.۵٪", percentageOrAmount = 2.5, pricePerKg = 85000.0, crudeProteinPercent = 0.0, category = "ویتامینه"),
            RationIngredient(name = "روغن گیاهی خوراکی", percentageOrAmount = 1.5, pricePerKg = 58000.0, crudeProteinPercent = 0.0, category = "روغن و چربی"),
            RationIngredient(name = "نمک و متیونین سنتتیک", percentageOrAmount = 2.0, pricePerKg = 48000.0, crudeProteinPercent = 30.0, category = "اسید آمینه")
        )

        // 3. مکمل ضد استرس و الکترولیت تابستانه
        val antiStressSupplementIngredients = listOf(
            RationIngredient(name = "ویتامین C پوشش‌دار گرید دارویی", percentageOrAmount = 20.0, pricePerKg = 280000.0, crudeProteinPercent = 0.0, category = "ویتامینه"),
            RationIngredient(name = "بتائین هیدروکلراید خالص", percentageOrAmount = 25.0, pricePerKg = 190000.0, crudeProteinPercent = 0.0, category = "دارویی"),
            RationIngredient(name = "ترکیب الکترولیت (KCl و NaHCO3)", percentageOrAmount = 35.0, pricePerKg = 45000.0, crudeProteinPercent = 0.0, category = "معدنی"),
            RationIngredient(name = "پروبیوتیک و حامل سبوس میکرونیزه", percentageOrAmount = 20.0, pricePerKg = 35000.0, crudeProteinPercent = 12.0, category = "دارویی")
        )

        val templates = listOf(
            RationPlan(
                name = "دان پیش‌دان استارتر گوشتی (۰ تا ۱۰ روزگی)",
                itemType = RationItemType.FEED,
                targetBird = "BROILER",
                targetStage = "استارتر",
                pricingMode = RationPricingMode.AUTO_FROM_INGREDIENTS,
                manualPricePerKg = 0.0,
                batchBaseKg = 100.0,
                ingredients = broilerStarterIngredients,
                notes = "فرمول استاندارد سویه راس ۳۰۸ و کاب ۵۰۰ با پروتئین حدود ۲۲٪ و انرژی قابل متابولیسم بالا جهت شروع پرشتاب رشد جوجه.",
                createdAtMillis = now,
                updatedAtMillis = now
            ),
            RationPlan(
                name = "دان تخم‌گذار مرغ بومی و صنعتی (فاز پیک تولید)",
                itemType = RationItemType.FEED,
                targetBird = "LAYER_HEN",
                targetStage = "تخم‌گذاری",
                pricingMode = RationPricingMode.AUTO_FROM_INGREDIENTS,
                manualPricePerKg = 0.0,
                batchBaseKg = 100.0,
                ingredients = layerPeakIngredients,
                notes = "فرمول ویژه حفظ راندمان تخم‌گذاری و ضخامت پوسته، غنی از کلسیم درشت دریایی و متیونین.",
                createdAtMillis = now - 1000,
                updatedAtMillis = now - 1000
            ),
            RationPlan(
                name = "مکمل ضد استرس گرمایی و الکترولیت تابستانه",
                itemType = RationItemType.SUPPLEMENT,
                targetBird = "ALL",
                targetStage = "مکمل تقویتی",
                pricingMode = RationPricingMode.AUTO_FROM_INGREDIENTS,
                manualPricePerKg = 0.0,
                batchBaseKg = 100.0,
                ingredients = antiStressSupplementIngredients,
                notes = "کنسانتره تخصصی جهت کاهش تلفات در دماهای بالای ۳۰ درجه و جلوگیری از آلکالوز تنفسی طیور.",
                createdAtMillis = now - 2000,
                updatedAtMillis = now - 2000
            )
        )

        for (plan in templates) {
            val cv = ContentValues().apply {
                put("name", plan.name)
                put("item_type", plan.itemType.name)
                put("target_bird", plan.targetBird)
                put("target_stage", plan.targetStage)
                put("pricing_mode", plan.pricingMode.name)
                put("manual_price_per_kg", plan.manualPricePerKg)
                put("batch_base_kg", plan.batchBaseKg)
                put("ingredients_json", serializeRationIngredients(plan.ingredients))
                put("notes", plan.notes)
                put("created_at_millis", plan.createdAtMillis)
                put("updated_at_millis", plan.updatedAtMillis)
            }
            db.insert("ration_plans", null, cv)
        }
    }

    fun getAllRationPlans(): List<RationPlan> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM ration_plans ORDER BY id DESC", null)
        val list = mutableListOf<RationPlan>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    RationPlan(
                        id = it.getLong(it.getColumnIndexOrThrow("id")),
                        name = it.getString(it.getColumnIndexOrThrow("name")),
                        itemType = RationItemType.fromString(it.getString(it.getColumnIndexOrThrow("item_type"))),
                        targetBird = it.getString(it.getColumnIndexOrThrow("target_bird")),
                        targetStage = it.getString(it.getColumnIndexOrThrow("target_stage")),
                        pricingMode = RationPricingMode.fromString(it.getString(it.getColumnIndexOrThrow("pricing_mode"))),
                        manualPricePerKg = it.getDouble(it.getColumnIndexOrThrow("manual_price_per_kg")),
                        batchBaseKg = it.getDouble(it.getColumnIndexOrThrow("batch_base_kg")),
                        ingredients = parseRationIngredients(it.getString(it.getColumnIndexOrThrow("ingredients_json"))),
                        notes = it.getString(it.getColumnIndexOrThrow("notes")),
                        createdAtMillis = it.getLong(it.getColumnIndexOrThrow("created_at_millis")),
                        updatedAtMillis = it.getLong(it.getColumnIndexOrThrow("updated_at_millis"))
                    )
                )
            }
        }
        return list
    }

    fun getRationPlanById(id: Long): RationPlan? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM ration_plans WHERE id = ?", arrayOf(id.toString()))
        cursor.use {
            if (it.moveToFirst()) {
                return RationPlan(
                    id = it.getLong(it.getColumnIndexOrThrow("id")),
                    name = it.getString(it.getColumnIndexOrThrow("name")),
                    itemType = RationItemType.fromString(it.getString(it.getColumnIndexOrThrow("item_type"))),
                    targetBird = it.getString(it.getColumnIndexOrThrow("target_bird")),
                    targetStage = it.getString(it.getColumnIndexOrThrow("target_stage")),
                    pricingMode = RationPricingMode.fromString(it.getString(it.getColumnIndexOrThrow("pricing_mode"))),
                    manualPricePerKg = it.getDouble(it.getColumnIndexOrThrow("manual_price_per_kg")),
                    batchBaseKg = it.getDouble(it.getColumnIndexOrThrow("batch_base_kg")),
                    ingredients = parseRationIngredients(it.getString(it.getColumnIndexOrThrow("ingredients_json"))),
                    notes = it.getString(it.getColumnIndexOrThrow("notes")),
                    createdAtMillis = it.getLong(it.getColumnIndexOrThrow("created_at_millis")),
                    updatedAtMillis = it.getLong(it.getColumnIndexOrThrow("updated_at_millis"))
                )
            }
        }
        return null
    }

    fun insertRationPlan(plan: RationPlan): Long {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("name", plan.name)
            put("item_type", plan.itemType.name)
            put("target_bird", plan.targetBird)
            put("target_stage", plan.targetStage)
            put("pricing_mode", plan.pricingMode.name)
            put("manual_price_per_kg", plan.manualPricePerKg)
            put("batch_base_kg", plan.batchBaseKg)
            put("ingredients_json", serializeRationIngredients(plan.ingredients))
            put("notes", plan.notes)
            put("created_at_millis", plan.createdAtMillis)
            put("updated_at_millis", System.currentTimeMillis())
        }
        val id = db.insert("ration_plans", null, cv)
        notifyDataChanged()
        return id
    }

    fun updateRationPlan(plan: RationPlan): Boolean {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("name", plan.name)
            put("item_type", plan.itemType.name)
            put("target_bird", plan.targetBird)
            put("target_stage", plan.targetStage)
            put("pricing_mode", plan.pricingMode.name)
            put("manual_price_per_kg", plan.manualPricePerKg)
            put("batch_base_kg", plan.batchBaseKg)
            put("ingredients_json", serializeRationIngredients(plan.ingredients))
            put("notes", plan.notes)
            put("updated_at_millis", System.currentTimeMillis())
        }
        val rows = db.update("ration_plans", cv, "id = ?", arrayOf(plan.id.toString()))
        notifyDataChanged()
        return rows > 0
    }

    fun deleteRationPlan(id: Long): Boolean {
        val db = writableDatabase
        val rows = db.delete("ration_plans", "id = ?", arrayOf(id.toString()))
        notifyDataChanged()
        return rows > 0
    }

    // ==================== MEDICATION WITHDRAWAL ====================

    fun updateFlockMedicationWithdrawal(flockId: Long, endMillis: Long?, medicationName: String?): Boolean {
        val db = writableDatabase
        val cv = ContentValues().apply {
            if (endMillis != null) put("medication_withdrawal_end_millis", endMillis) else putNull("medication_withdrawal_end_millis")
            if (medicationName != null) put("active_medication_name", medicationName) else putNull("active_medication_name")
        }
        val rows = db.update("flocks", cv, "id = ?", arrayOf(flockId.toString()))
        notifyDataChanged()
        return rows > 0
    }

    // ==================== BIOSECURITY CHECKS ====================

    private fun ensureDefaultBiosecurityChecks(db: SQLiteDatabase) {
        val defaultItems = listOf(
            // پاییز
            Triple("BIO_AUTUMN_1", "AUTUMN", "بررسی توری‌ها و بستن تمام منافذ ورودی سالن جهت جلوگیری از نفوذ گنجشک و پرندگان وحشی مهاجر"),
            Triple("BIO_AUTUMN_2", "AUTUMN", "ضدعفونی حوضچه ورودی خودروهای حامل دان و کارتن تخم‌مرغ با محلول فرمالین یا گلوتارآلدئید"),
            Triple("BIO_AUTUMN_3", "AUTUMN", "پایش تنفس شبانه پرندگان و نمونه‌برداری فوری در صورت مشاهده خس‌خس یا ترشحات بینی (خطر آنفلوآنزا)"),
            // بهار
            Triple("BIO_SPRING_1", "SPRING", "سم‌پاشی زیر قفس‌ها و درزهای دیوار با سموم مجاز برای پیشگیری از خروج مایت قرمز (کنه مرغی)"),
            Triple("BIO_SPRING_2", "SPRING", "شستشو و ضدعفونی خطوط آبخوری نیپل با اسید سیتریک یا پرواستیک جهت حذف بیوفیلم زمستانه"),
            Triple("BIO_SPRING_3", "SPRING", "تنظیم ساعات روشنایی متناسب با طولانی شدن طبیعی روز جهت تثبیت تولید تخم‌مرغ"),
            // تابستان
            Triple("BIO_SUMMER_1", "SUMMER", "سرویس الکتروموتورهای فن و ژنراتور اضطراری برق به همراه تست ترموستات اضطراری آلارم دما"),
            Triple("BIO_SUMMER_2", "SUMMER", "سرویس پد خنک‌کننده (Cooling Pad) و اطمینان از خیس شدن یکنواخت تمام پوشال‌ها"),
            Triple("BIO_SUMMER_3", "SUMMER", "افزودن الکترولیت یا ویتامین C به آبخوری در ساعات ۱۰ صبح تا ۵ عصر در روزهای گرم بالای ۳۳ درجه"),
            // زمستان
            Triple("BIO_WINTER_1", "WINTER", "بررسی حداقل تهویه (Minimum Ventilation) برای تخلیه آمونیاک بدون ایجاد افت دما در سالن"),
            Triple("BIO_WINTER_2", "WINTER", "بررسی رطوبت بستر و زیرآب‌خوری‌ها برای جلوگیری از کوکسیدیوز و تاول سینه"),
            Triple("BIO_WINTER_3", "WINTER", "عایق‌بندی لوله‌های آب ورودی فارم جهت جلوگیری از یخ‌زدگی در شب‌های یخبندان")
        )

        for (item in defaultItems) {
            val cv = ContentValues().apply {
                put("id", item.first)
                put("season", item.second)
                put("title", item.third)
                put("description", "دستورالعمل حیاتی پایش پیشگیرانه و بیوامنیتی فارم طیور")
                put("priority", "HIGH")
                put("is_completed", 0)
            }
            db.insertWithOnConflict("biosecurity_checks", null, cv, SQLiteDatabase.CONFLICT_IGNORE)
        }
    }

    fun getBiosecurityChecks(season: com.example.poultryapp.engine.FarmSeason? = null): List<BiosecurityCheckItem> {
        val db = readableDatabase
        val query = if (season != null) {
            "SELECT * FROM biosecurity_checks WHERE season = ?"
        } else {
            "SELECT * FROM biosecurity_checks ORDER BY id ASC"
        }
        val args = if (season != null) arrayOf(season.name) else null
        val cursor = db.rawQuery(query, args)
        val list = mutableListOf<BiosecurityCheckItem>()
        cursor.use {
            while (it.moveToNext()) {
                val sName = it.getString(it.getColumnIndexOrThrow("season"))
                val farmSeason = try { com.example.poultryapp.engine.FarmSeason.valueOf(sName) } catch (e: Exception) { com.example.poultryapp.engine.FarmSeason.AUTUMN }
                list.add(
                    BiosecurityCheckItem(
                        id = it.getString(it.getColumnIndexOrThrow("id")),
                        season = farmSeason,
                        title = it.getString(it.getColumnIndexOrThrow("title")),
                        description = it.getString(it.getColumnIndexOrThrow("description")),
                        priority = it.getString(it.getColumnIndexOrThrow("priority")),
                        isCompleted = it.getInt(it.getColumnIndexOrThrow("is_completed")) == 1,
                        completedDateJalaliStr = it.getString(it.getColumnIndexOrThrow("completed_date_jalali_str"))
                    )
                )
            }
        }
        return list
    }

    fun toggleBiosecurityCheck(id: String, isCompleted: Boolean, dateJalaliStr: String? = null): Boolean {
        val db = writableDatabase
        val cv = ContentValues().apply {
            put("is_completed", if (isCompleted) 1 else 0)
            if (isCompleted && dateJalaliStr != null) {
                put("completed_date_jalali_str", dateJalaliStr)
            } else if (!isCompleted) {
                putNull("completed_date_jalali_str")
            }
        }
        val rows = db.update("biosecurity_checks", cv, "id = ?", arrayOf(id))
        notifyDataChanged()
        return rows > 0
    }



    fun autoAddProducedEggsToWarehouse(
        flockId: Long,
        flockName: String,
        fertileCount: Int,
        tableCount: Int,
        dateJalaliStr: String
    ): Boolean {
        val items = getAllWarehouseItems()
        var updated = false
        if (fertileCount > 0) {
            val fertileName = "تخم‌مرغ نطفه‌دار $flockName"
            val existing = items.firstOrNull { it.name == fertileName && it.category == WarehouseCategory.EGG }
            if (existing != null) {
                updateWarehouseItem(existing.copy(currentStock = existing.currentStock + fertileCount))
            } else {
                insertWarehouseItem(
                    WarehouseItem(
                        name = fertileName,
                        category = WarehouseCategory.EGG,
                        currentStock = fertileCount.toDouble(),
                        unit = "عدد",
                        pricePerUnit = 12000.0,
                        assignedFlockIds = listOf(flockId),
                        isForSale = true,
                        notes = "شارژ خودکار از تولید روزانه گله $flockName"
                    )
                )
            }
            updated = true
        }
        if (tableCount > 0) {
            val tableName = "تخم‌مرغ خوراکی $flockName"
            val existing = items.firstOrNull { it.name == tableName && it.category == WarehouseCategory.EGG }
            if (existing != null) {
                updateWarehouseItem(existing.copy(currentStock = existing.currentStock + tableCount))
            } else {
                insertWarehouseItem(
                    WarehouseItem(
                        name = tableName,
                        category = WarehouseCategory.EGG,
                        currentStock = tableCount.toDouble(),
                        unit = "عدد",
                        pricePerUnit = 4500.0,
                        assignedFlockIds = listOf(flockId),
                        isForSale = true,
                        notes = "شارژ خودکار از تولید روزانه گله $flockName"
                    )
                )
            }
            updated = true
        }
        if (updated) notifyDataChanged()
        return updated
    }
}


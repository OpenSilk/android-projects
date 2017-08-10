/*
 * Copyright (c) 2016 OpenSilk Productions LLC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.opensilk.common.content

import android.content.SharedPreferences

/**
 * Created by drew on 4/30/15.
 */
abstract class PreferencesWrapper
constructor(
        protected val prefs: SharedPreferences
) {

    fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    fun putLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }

    fun putInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun getBoolean(key: String, def: Boolean): Boolean {
        return prefs.getBoolean(key, def)
    }

    fun getLong(key: String, def: Long): Long {
        return prefs.getLong(key, def)
    }

    fun getInt(key: String, def: Int): Int {
        return prefs.getInt(key, def)
    }

    fun getString(key: String, def: String): String {
        return prefs.getString(key, def)
    }

    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    fun apply(transaction: Transaction) {
        val editor = prefs.edit();
        try {
            for (op in transaction.boolOps) {
                editor.putBoolean(op.key, op.value)
            }
            for (op in transaction.longOps) {
                editor.putLong(op.key, op.value)
            }
            for (op in transaction.intOps) {
                editor.putInt(op.key, op.value)
            }
            for (op in transaction.stringOps) {
                editor.putString(op.key, op.value)
            }
        } finally {
            editor.apply()
        }
    }

    class Transaction {
        internal var boolOps: Array<BoolOp> = emptyArray();
        internal var longOps: Array<LongOp> = emptyArray();
        internal var intOps: Array<IntOp> = emptyArray();
        internal var stringOps: Array<StringOp> = emptyArray();

        fun putBoolean(key: String, value: Boolean): Transaction {
            boolOps += BoolOp(key, value);
            return this@Transaction
        }

        fun putLong(key: String, value: Long): Transaction {
            longOps += LongOp(key, value);
            return this@Transaction
        }

        fun putInt(key: String, value: Int): Transaction {
            intOps += IntOp(key, value);
            return this@Transaction
        }

        fun putString(key: String, value: String): Transaction {
            stringOps += StringOp(key, value)
            return this@Transaction
        }
    }

    internal class BoolOp constructor(val key: String, val value: Boolean);
    internal class LongOp constructor(val key: String, val value: Long);
    internal class IntOp constructor(val key: String, val value: Int);
    internal class StringOp constructor(val key: String, val value: String);

}

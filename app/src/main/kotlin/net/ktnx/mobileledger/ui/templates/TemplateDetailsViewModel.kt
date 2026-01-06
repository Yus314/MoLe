/*
 * Copyright © 2024 Damyan Ivanov.
 * This file is part of MoLe.
 * MoLe is free software: you can distribute it and/or modify it
 * under the term of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your opinion), any later version.
 *
 * MoLe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License terms for details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MoLe. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ktnx.mobileledger.ui.templates

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import net.ktnx.mobileledger.BuildConfig
import net.ktnx.mobileledger.dao.BaseDAO
import net.ktnx.mobileledger.db.DB
import net.ktnx.mobileledger.db.TemplateAccount
import net.ktnx.mobileledger.model.TemplateDetailsItem
import net.ktnx.mobileledger.utils.Logger
import net.ktnx.mobileledger.utils.Misc
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

class TemplateDetailsViewModel : ViewModel() {

    private val items = MutableLiveData<List<TemplateDetailsItem>>(emptyList())
    private val syntheticItemId = AtomicInteger(0)
    private var mPatternId: Long? = null
    private var itemsLoaded = false

    var defaultTemplateName: String? = null

    fun resetItems() {
        applyList(ArrayList())
    }

    fun applyList(srcList: List<TemplateDetailsItem>?) {
        applyList(srcList, false)
    }

    fun applyList(srcList: List<TemplateDetailsItem>?, async: Boolean) {
        var workList = srcList
        var changes: Boolean

        if (workList == null) {
            workList = ArrayList(items.value)
            changes = false
        } else {
            changes = true
        }

        workList = workList.toList()

        if (BuildConfig.DEBUG) {
            Logger.debug(TAG, "Considering old list")
            for (item in workList) {
                Logger.debug(TAG, String.format(Locale.US, " id %d pos %d", item.id, item.position))
            }
        }

        val newList = ArrayList<TemplateDetailsItem>()
        var hasEmptyItem = false

        if (workList.isEmpty()) {
            val header = TemplateDetailsItem.createHeader()
            header.id = 0L
            newList.add(header)
            changes = true
        } else {
            newList.add(workList[0])
        }

        for (i in 1 until workList.size) {
            val accRow = workList[i].asAccountRowItem()
            when {
                accRow.isEmpty() -> {
                    // it is normal to have two empty rows if they are at the
                    // top (position 1 and 2)
                    if (!hasEmptyItem || i < 3) {
                        accRow.position = newList.size.toLong()
                        newList.add(accRow)
                    } else {
                        changes = true // row skipped
                    }
                    hasEmptyItem = true
                }
                else -> {
                    accRow.position = newList.size.toLong()
                    newList.add(accRow)
                }
            }
        }

        while (newList.size < 3) {
            val accountRow = TemplateDetailsItem.createAccountRow()
            accountRow.id = genItemId().toLong()
            accountRow.position = newList.size.toLong()
            newList.add(accountRow)
            changes = true
            hasEmptyItem = true
        }

        if (!hasEmptyItem) {
            val accountRow = TemplateDetailsItem.createAccountRow()
            accountRow.id = genItemId().toLong()
            accountRow.position = newList.size.toLong()
            newList.add(accountRow)
            changes = true
        }

        when {
            changes -> {
                Logger.debug(TAG, "Changes detected, applying new list")
                if (async) {
                    items.postValue(newList)
                } else {
                    items.setValue(newList)
                }
            }
            else -> Logger.debug(TAG, "No changes, ignoring new list")
        }
    }

    fun genItemId(): Int = syntheticItemId.decrementAndGet()

    fun getItems(patternId: Long?): LiveData<List<TemplateDetailsItem>> {
        if (itemsLoaded && patternId == mPatternId) {
            return items
        }

        if (patternId != null && patternId <= 0) {
            throw IllegalArgumentException("Pattern ID $patternId is invalid")
        }

        mPatternId = patternId

        if (mPatternId == null) {
            resetItems()
            itemsLoaded = true
            return items
        }

        val patternId = mPatternId ?: return items
        val db = DB.get()
        val dbList = db.getTemplateDAO().getTemplateWithAccounts(patternId)
        dbList.observeForever(object : androidx.lifecycle.Observer<net.ktnx.mobileledger.db.TemplateWithAccounts> {
            override fun onChanged(src: net.ktnx.mobileledger.db.TemplateWithAccounts) {
                val l = ArrayList<TemplateDetailsItem>()

                val header = TemplateDetailsItem.fromRoomObject(src.header)
                Logger.debug(DB_TAG, "Got header template item with id of ${header.id}")
                l.add(header)

                src.accounts.sortedBy { it.position }.forEach { acc ->
                    l.add(TemplateDetailsItem.fromRoomObject(acc))
                }

                for (item in l) {
                    Logger.debug(DB_TAG, "Loaded pattern item $item")
                }

                applyList(l, true)
                itemsLoaded = true

                dbList.removeObserver(this)
            }
        })

        return items
    }

    fun setTestText(text: String) {
        val list = ArrayList(items.value)
        val header = TemplateDetailsItem.Header(list[0].asHeaderItem())
        header.testText = text
        list[0] = header

        items.setValue(list)
    }

    fun onSaveTemplate() {
        Logger.debug("flow", "PatternDetailsViewModel.onSavePattern(); model=$this")
        val list = requireNotNull(items.value)

        BaseDAO.runAsync {
            val newPattern = mPatternId == null || (mPatternId ?: 0) <= 0

            val modelHeader = list[0].asHeaderItem()

            modelHeader.name = Misc.trim(modelHeader.name) ?: ""
            if (modelHeader.name.isEmpty()) {
                modelHeader.name = defaultTemplateName ?: ""
            }

            val headerDAO = DB.get().getTemplateDAO()
            val dbHeader = modelHeader.toDBO()

            if (newPattern) {
                dbHeader.id = 0L
                dbHeader.id = headerDAO.insertSync(dbHeader)
                mPatternId = dbHeader.id
            } else {
                headerDAO.updateSync(dbHeader)
            }

            Logger.debug("pattern-db",
                String.format(Locale.US, "Stored pattern header %d, item=%s", dbHeader.id,
                    modelHeader))

            val savedPatternId = mPatternId ?: return@runAsync
            val taDAO = DB.get().getTemplateAccountDAO()
            taDAO.prepareForSave(savedPatternId)

            for (i in 1 until list.size) {
                val accRowItem = list[i].asAccountRowItem()
                val dbAccount = accRowItem.toDBO(dbHeader.id)
                dbAccount.templateId = savedPatternId
                dbAccount.position = i.toLong()

                if (dbAccount.id < 0) {
                    dbAccount.id = 0
                    dbAccount.id = taDAO.insertSync(dbAccount)
                } else {
                    taDAO.updateSync(dbAccount)
                }

                Logger.debug("pattern-db", String.format(Locale.US,
                    "Stored pattern account %d, account=%s, comment=%s, neg=%s, item=%s",
                    dbAccount.id, dbAccount.accountName,
                    dbAccount.accountComment, dbAccount.negateAmount, accRowItem))
            }

            taDAO.finishSave(savedPatternId)
        }
    }

    private fun copyItems(): ArrayList<TemplateDetailsItem> {
        val oldList = items.value ?: return ArrayList()

        val result = ArrayList<TemplateDetailsItem>(oldList.size)

        for (item in oldList) {
            when (item) {
                is TemplateDetailsItem.Header ->
                    result.add(TemplateDetailsItem.Header(item.asHeaderItem()))
                is TemplateDetailsItem.AccountRow ->
                    result.add(TemplateDetailsItem.AccountRow(item.asAccountRowItem()))
                else -> throw RuntimeException("Unexpected item $item")
            }
        }

        return result
    }

    fun moveItem(sourcePos: Int, targetPos: Int) {
        val newList = copyItems()

        if (BuildConfig.DEBUG) {
            Logger.debug("drag", "Before move:")
            for (i in 1 until newList.size) {
                val item = newList[i]
                Logger.debug("drag",
                    String.format(Locale.US, "  %d: id %d, pos %d", i, item.id, item.position))
            }
        }

        val item = newList.removeAt(sourcePos)
        newList.add(targetPos, item)

        // adjust affected items' positions
        val startPos: Int
        val endPos: Int
        if (sourcePos < targetPos) {
            // moved down
            startPos = sourcePos
            endPos = targetPos
        } else {
            // moved up
            startPos = targetPos
            endPos = sourcePos
        }

        for (i in startPos..endPos) {
            newList[i].position = i.toLong()
        }

        if (BuildConfig.DEBUG) {
            Logger.debug("drag", "After move:")
            for (i in 1 until newList.size) {
                val item2 = newList[i]
                Logger.debug("drag",
                    String.format(Locale.US, "  %d: id %d, pos %d", i, item2.id, item2.position))
            }
        }

        items.setValue(newList)
    }

    fun removeItem(position: Int) {
        Logger.debug(TAG, "Removing item at position $position")
        val newList = copyItems()
        newList.removeAt(position)
        for (i in position until newList.size) {
            newList[i].position = i.toLong()
        }
        applyList(newList)
    }

    companion object {
        const val TAG = "template-details-model"
        const val DB_TAG = "$TAG-db"
    }
}

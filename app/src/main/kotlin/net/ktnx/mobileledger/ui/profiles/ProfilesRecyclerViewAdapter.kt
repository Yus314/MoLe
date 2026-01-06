/*
 * Copyright © 2021 Damyan Ivanov.
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

package net.ktnx.mobileledger.ui.profiles

import android.annotation.SuppressLint
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import java.util.Collections
import net.ktnx.mobileledger.R
import net.ktnx.mobileledger.db.DB
import net.ktnx.mobileledger.db.Profile
import net.ktnx.mobileledger.model.Data
import net.ktnx.mobileledger.utils.Colors
import net.ktnx.mobileledger.utils.Logger.debug

class ProfilesRecyclerViewAdapter : RecyclerView.Adapter<ProfilesRecyclerViewAdapter.ProfileListViewHolder>() {
    @JvmField
    val editingProfiles = MutableLiveData(false)
    private val rearrangeHelper: ItemTouchHelper
    private val listDiffer: AsyncListDiffer<Profile>
    private var recyclerView: RecyclerView? = null
    private var animationsEnabled = true

    init {
        debug("flow", "ProfilesRecyclerViewAdapter.new()")

        setHasStableIds(true)
        listDiffer = AsyncListDiffer(
            this,
            object : DiffUtil.ItemCallback<Profile>() {
            override fun areItemsTheSame(oldItem: Profile, newItem: Profile): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Profile, newItem: Profile): Boolean {
                return oldItem == newItem
            }
        }
        )

        val cb = object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                return makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val profiles = ArrayList(listDiffer.currentList)
                Collections.swap(
                    profiles,
                    viewHolder.bindingAdapterPosition,
                    target.bindingAdapterPosition
                )
                DB.get()
                    .getProfileDAO()
                    .updateOrder(profiles, null)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, i: Int) {
            }
        }
        rearrangeHelper = ItemTouchHelper(cb)
    }

    override fun getItemId(position: Int): Long {
        return listDiffer.currentList[position].id
    }

    fun setProfileList(list: List<Profile>) {
        listDiffer.submitList(list)
    }

    fun setAnimationsEnabled(animationsEnabled: Boolean) {
        this.animationsEnabled = animationsEnabled
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        rearrangeHelper.attachToRecyclerView(null)
        super.onDetachedFromRecyclerView(recyclerView)
        this.recyclerView = null
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
        if (editingProfiles()) {
            rearrangeHelper.attachToRecyclerView(recyclerView)
        }
    }

    fun editingProfiles(): Boolean {
        return editingProfiles.value ?: false
    }

    fun startEditingProfiles() {
        if (editingProfiles()) return
        editingProfiles.setValue(true)
        rearrangeHelper.attachToRecyclerView(recyclerView)
    }

    fun stopEditingProfiles() {
        if (!editingProfiles()) return
        editingProfiles.setValue(false)
        rearrangeHelper.attachToRecyclerView(null)
    }

    fun flipEditingProfiles() {
        if (editingProfiles()) {
            stopEditingProfiles()
        } else {
            startEditingProfiles()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProfileListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.profile_list_content, parent, false)
        return ProfileListViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProfileListViewHolder, position: Int) {
        val profile = listDiffer.currentList[position]
        val currentProfile = Data.getProfile()

        val hue = profile.theme
        if (hue == -1) {
            holder.mColorTag.setBackgroundColor(
                Colors.getPrimaryColorForHue(Colors.DEFAULT_HUE_DEG)
            )
        } else {
            holder.mColorTag.setBackgroundColor(Colors.getPrimaryColorForHue(hue))
        }

        holder.mTitle.text = profile.name

        holder.mEditButton.setOnClickListener { view ->
            val p = listDiffer.currentList[holder.bindingAdapterPosition]
            ProfileDetailActivity.start(view.context, p)
        }

        val sameProfile = currentProfile != null && currentProfile.id == profile.id
        holder.itemView.background = if (sameProfile) ColorDrawable(Colors.tableRowDarkBG) else null

        if (editingProfiles()) {
            val wasHidden = holder.mEditButton.visibility == View.GONE
            holder.mRearrangeHandle.visibility = View.VISIBLE
            holder.mEditButton.visibility = View.VISIBLE
            if (wasHidden && animationsEnabled) {
                val a = AnimationUtils.loadAnimation(
                    holder.mRearrangeHandle.context,
                    R.anim.fade_in
                )
                holder.mRearrangeHandle.startAnimation(a)
                holder.mEditButton.startAnimation(a)
            }
        } else {
            val wasShown = holder.mEditButton.visibility == View.VISIBLE
            holder.mRearrangeHandle.visibility = View.INVISIBLE
            holder.mEditButton.visibility = View.GONE
            if (wasShown && animationsEnabled) {
                val a = AnimationUtils.loadAnimation(
                    holder.mRearrangeHandle.context,
                    R.anim.fade_out
                )
                holder.mRearrangeHandle.startAnimation(a)
                holder.mEditButton.startAnimation(a)
            }
        }
    }

    override fun getItemCount(): Int {
        return listDiffer.currentList.size
    }

    @SuppressLint("ClickableViewAccessibility")
    inner class ProfileListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val mEditButton: TextView = view.findViewById(R.id.profile_list_edit_button)
        val mTitle: TextView = view.findViewById(R.id.title)
        val mColorTag: TextView = view.findViewById(R.id.colorTag)
        val tagAndHandleLayout: LinearLayout = view.findViewById(R.id.handle_and_tag)
        val mRearrangeHandle: ImageView = view.findViewById(R.id.profile_list_rearrange_handle)
        val mRow: ConstraintLayout = view as ConstraintLayout

        init {
            mRow.setOnClickListener { onProfileRowClicked(it) }
            mTitle.setOnClickListener { v ->
                val row = v.parent as View
                onProfileRowClicked(row)
            }
            mColorTag.setOnClickListener { v ->
                val row = (v.parent as View).parent as View
                onProfileRowClicked(row)
            }
            mTitle.setOnLongClickListener {
                flipEditingProfiles()
                true
            }

            val dragStarter = View.OnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN && editingProfiles()) {
                    rearrangeHelper.startDrag(this)
                    true
                } else {
                    false
                }
            }

            tagAndHandleLayout.setOnTouchListener(dragStarter)
        }

        private fun onProfileRowClicked(v: View) {
            if (editingProfiles()) return
            val profile = listDiffer.currentList[bindingAdapterPosition]
            if (Data.getProfile() != profile) {
                debug("profiles", "Setting profile to " + profile.name)
                Data.drawerOpen.setValue(false)
                Data.setCurrentProfile(profile)
            } else {
                debug(
                    "profiles",
                    "Not setting profile to the current profile " + profile.name
                )
            }
        }
    }
}

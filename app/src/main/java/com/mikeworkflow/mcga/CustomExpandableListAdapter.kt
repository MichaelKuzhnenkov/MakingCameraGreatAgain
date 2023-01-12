package com.mikeworkflow.mcga

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.TextView

class CustomExpandableListAdapter(context : Context, title : ArrayList<String>,
                                    data : HashMap<String, ArrayList<String>>) : BaseExpandableListAdapter() {

    var context : Context = context
    var titleList : ArrayList<String> = title
    var dataMap : HashMap<String, ArrayList<String>> = data

    override fun getGroupCount(): Int {
        return titleList.size
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        return dataMap[titleList[groupPosition]]!!.size
    }

    override fun getGroup(groupPosition: Int): Any {
        return titleList[groupPosition]
    }

    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        return dataMap[titleList[groupPosition]]!![childPosition]
    }

    override fun getGroupId(groupPosition: Int): Long {
        return groupPosition.toLong()
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return childPosition.toLong()
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun getGroupView(
        groupPosition: Int,
        isExpanded: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        val listTitle = getGroup(groupPosition) as String
        val layoutInflater = this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val returnConvertView = layoutInflater.inflate(R.layout.support_simple_spinner_dropdown_item, null)
        val expandedListTextView = returnConvertView.findViewById<TextView>(android.R.id.text1)
        expandedListTextView.text = listTitle
        return returnConvertView
    }

    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        val elementText = getChild(groupPosition, childPosition) as String
        val layoutInflater = this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val returnConvertView = layoutInflater.inflate(R.layout.support_simple_spinner_dropdown_item, null)
        val expandedListTextView = returnConvertView.findViewById<TextView>(android.R.id.text1)
        expandedListTextView.text = elementText
        return returnConvertView
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return true
    }
}
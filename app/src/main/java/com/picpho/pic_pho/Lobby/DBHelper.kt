package com.picpho.pic_pho.Lobby

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper (context: Context?,
                name: String?,
                factory: SQLiteDatabase.CursorFactory?,
                version: Int
) : SQLiteOpenHelper(context, name, factory, version) {

    val COL_ID = "_id"

    val TABLE_NAME_GROUP = "Groups"
    val COL_GROUP_NAME = "groupName"
    val COL_GROUP_PRESENTIMAGE = "presentImage"
    val COL_GROUP_ABSOLUTEPATHLIST = "absolutePathList"
    val COL_GROUP_EVENTDATE = "eventDate"
    val COL_GROUP_MEMBERLIST = "memberList"
    val COL_GROUP_ISDELETED = "isDeleted"

    val TABLE_NAME_FRIEND = "Friends"
    val COL_FRIEND_NAME = "userName"
    val COL_FRIEND_PROFILEIMG = "userProfileImage"
    val COL_FRIEND_UID = "userUid"

    val TABLE_NAME_PHOTO = "Photos"
    val COL_PHOTO_ABSOLUTEPATH = "photoAbsolutePath"


    override fun onCreate(db: SQLiteDatabase?) {
        val createGroupTable = "CREATE TABLE IF NOT EXISTS $TABLE_NAME_GROUP ($COL_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COL_GROUP_NAME VARCHAR(50)," +
                "$COL_GROUP_PRESENTIMAGE TEXT, " +
                "$COL_GROUP_EVENTDATE VARCHAR(10), " +
                "$COL_GROUP_ABSOLUTEPATHLIST TEXT, " +
                "$COL_GROUP_MEMBERLIST TEXT," +
                "$COL_GROUP_ISDELETED BOOLEAN)"

        val createFriendTable = "CREATE TABLE IF NOT EXISTS $TABLE_NAME_FRIEND ($COL_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COL_FRIEND_NAME VARCHAR(50)," +
                "$COL_FRIEND_PROFILEIMG TEXT, " +
                "$COL_FRIEND_UID TEXT)"

        val createPhotoTable = "CREATE TABLE IF NOT EXISTS $TABLE_NAME_PHOTO ($COL_ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "$COL_PHOTO_ABSOLUTEPATH TEXT)"

        db!!.execSQL(createGroupTable)
        db!!.execSQL(createFriendTable)
        db!!.execSQL(createPhotoTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        val query0 = "DROP TABLE if exists $TABLE_NAME_GROUP"
        val query1 = "DROP TABLE if exists $TABLE_NAME_FRIEND"
        val query2 = "DROP TABLE if exists  $TABLE_NAME_PHOTO"
        db!!.execSQL(query0)
        db!!.execSQL(query1)
        db!!.execSQL(query2)
        onCreate(db)
    }
}
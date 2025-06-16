package dev.usbharu.stl.model


import org.jetbrains.exposed.sql.Table

/**
 * データベースにセッション情報を格納するためのテーブル定義
 */
object Sessions : Table() {
    val id = varchar("id", 128)
    val data = text("data")
    // 最終更新日時を記録するカラムを追加 (Unixミリ秒タイムスタンプ)
    val updatedAt = long("updated_at")
    override val primaryKey = PrimaryKey(id)
}

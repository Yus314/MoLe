/*
 * Copyright © 2026 Damyan Ivanov.
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

package net.ktnx.mobileledger.domain.model

/**
 * データベース操作に関するエラー
 *
 * Room/SQLiteの操作で発生するエラーを表す。
 * 基本的にリトライ不可（ConnectionFailed除く）。
 */
sealed class DatabaseError : AppError() {
    override val isRetryable: Boolean = false

    /**
     * 制約違反エラー
     *
     * - UNIQUE制約違反
     * - FOREIGN KEY制約違反
     * - NOT NULL制約違反
     */
    data class ConstraintViolation(
        override val message: String = "データの整合性エラーが発生しました",
        override val cause: Throwable? = null,
        val constraintName: String? = null
    ) : DatabaseError()

    /**
     * クエリ実行エラー
     *
     * - SQL構文エラー
     * - テーブル/カラム不存在
     * - その他のSQLiteエラー
     */
    data class QueryFailed(
        override val message: String = "データベースクエリに失敗しました",
        override val cause: Throwable? = null
    ) : DatabaseError()

    /**
     * 接続エラー
     *
     * - データベースファイルオープン失敗
     * - ディスク容量不足
     *
     * このエラーはリトライ可能（一時的な問題の可能性）
     */
    data class ConnectionFailed(
        override val message: String = "データベースに接続できません",
        override val cause: Throwable? = null
    ) : DatabaseError() {
        override val isRetryable: Boolean = true
    }

    /**
     * データ未検出エラー
     *
     * - 指定されたIDのレコードが存在しない
     * - 期待されるデータが見つからない
     */
    data class NotFound(
        override val message: String = "データが見つかりません",
        val entityType: String? = null,
        val entityId: Any? = null
    ) : DatabaseError() {
        override val cause: Throwable? = null
    }

    /**
     * マイグレーションエラー
     *
     * - データベースバージョンアップ時の移行失敗
     */
    data class MigrationFailed(
        override val message: String = "データベースの移行に失敗しました",
        override val cause: Throwable? = null,
        val fromVersion: Int = 0,
        val toVersion: Int = 0
    ) : DatabaseError()
}

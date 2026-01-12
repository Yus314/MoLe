/**
 * CurrencyFormatter - 通貨・数値フォーマットサービスの契約
 *
 * このファイルは実装の契約（インターフェース）を定義する。
 * 実際の実装は app/src/main/kotlin/net/ktnox/mobileledger/service/ に配置する。
 *
 * Date: 2026-01-12
 * Feature: 009-eliminate-data-singleton
 */
package net.ktnox.mobileledger.service

import java.util.Locale
import kotlinx.coroutines.flow.StateFlow
import net.ktnx.mobileledger.model.Currency

/**
 * 通貨フォーマット設定
 *
 * @property locale 現在のロケール
 * @property symbolPosition 通貨記号の位置
 * @property hasGap 記号と数値の間にスペースを入れるか
 * @property decimalSeparator 小数点区切り文字
 * @property groupingSeparator 桁区切り文字
 */
data class CurrencyFormatConfig(
    val locale: Locale,
    val symbolPosition: Currency.Position,
    val hasGap: Boolean,
    val decimalSeparator: Char,
    val groupingSeparator: Char
) {
    companion object {
        /**
         * ロケールからデフォルト設定を生成
         */
        fun fromLocale(locale: Locale): CurrencyFormatConfig {
            val symbols = java.text.DecimalFormatSymbols.getInstance(locale)
            return CurrencyFormatConfig(
                locale = locale,
                symbolPosition = Currency.Position.before,
                hasGap = true,
                decimalSeparator = symbols.decimalSeparator,
                groupingSeparator = symbols.groupingSeparator
            )
        }
    }
}

/**
 * 通貨・数値フォーマットサービスのインターフェース
 *
 * ロケール依存の通貨・数値フォーマットを提供する。
 * @Singleton スコープで管理され、ロケール変更時に全画面で反映される。
 *
 * ## 使用例
 *
 * ```kotlin
 * // ViewModel での使用
 * @HiltViewModel
 * class TransactionViewModel @Inject constructor(
 *     private val currencyFormatter: CurrencyFormatter
 * ) : ViewModel() {
 *     fun formatAmount(amount: Float): String {
 *         return currencyFormatter.formatCurrency(amount)
 *     }
 * }
 *
 * // Compose での使用
 * @Composable
 * fun AmountText(amount: Float, formatter: CurrencyFormatter) {
 *     val locale by formatter.locale.collectAsState()
 *     Text(text = formatter.formatCurrency(amount))
 * }
 * ```
 *
 * ## Application での初期化
 *
 * ```kotlin
 * @HiltAndroidApp
 * class App : Application() {
 *     @Inject lateinit var currencyFormatter: CurrencyFormatter
 *
 *     override fun onCreate() {
 *         super.onCreate()
 *         currencyFormatter.refresh(Locale.getDefault())
 *     }
 *
 *     override fun onConfigurationChanged(newConfig: Configuration) {
 *         super.onConfigurationChanged(newConfig)
 *         currencyFormatter.refresh(Locale.getDefault())
 *     }
 * }
 * ```
 */
interface CurrencyFormatter {
    /**
     * 現在のロケール
     */
    val locale: StateFlow<Locale>

    /**
     * 通貨フォーマット設定
     */
    val config: StateFlow<CurrencyFormatConfig>

    /**
     * 通貨記号の位置
     */
    val currencySymbolPosition: StateFlow<Currency.Position>

    /**
     * 記号と数値の間にスペースを入れるか
     */
    val currencyGap: StateFlow<Boolean>

    /**
     * 数値を通貨形式でフォーマットする
     *
     * 例: 1234.56 → "¥1,234.56" (ja-JP) or "1.234,56 €" (de-DE)
     *
     * @param amount フォーマットする金額
     * @param currencySymbol 通貨記号（省略時は記号なし）
     * @return フォーマット済み文字列
     */
    fun formatCurrency(amount: Float, currencySymbol: String? = null): String

    /**
     * 数値をロケールに従ってフォーマットする
     *
     * 例: 1234.56 → "1,234.56" (en-US) or "1.234,56" (de-DE)
     *
     * @param number フォーマットする数値
     * @return フォーマット済み文字列
     */
    fun formatNumber(number: Float): String

    /**
     * ロケールに従って文字列を数値にパースする
     *
     * 例: "1,234.56" (en-US) or "1.234,56" (de-DE) → 1234.56
     *
     * @param str パースする文字列
     * @return パースされた数値
     * @throws NumberFormatException パースに失敗した場合
     */
    fun parseNumber(str: String): Float

    /**
     * 現在のロケールの小数点区切り文字を取得する
     *
     * @return 小数点区切り文字
     */
    fun getDecimalSeparator(): String

    /**
     * 現在のロケールの桁区切り文字を取得する
     *
     * @return 桁区切り文字
     */
    fun getGroupingSeparator(): String

    /**
     * ロケールに基づいて通貨フォーマット設定を更新する
     *
     * @param locale 新しいロケール
     */
    fun refresh(locale: Locale)

    /**
     * hledger サーバーから取得した AmountStyle に基づいて設定を更新する
     *
     * @param symbolPosition 通貨記号の位置
     * @param hasGap 記号と数値の間にスペースを入れるか
     */
    fun updateFromAmountStyle(symbolPosition: Currency.Position, hasGap: Boolean)
}

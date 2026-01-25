/*
 * Temporary typealiases to keep compatibility for code/tests that still
 * reference repository interfaces under the data.repository package.
 * These point to the moved interfaces in domain.repository.
 */
package net.ktnx.mobileledger.data.repository

typealias AccountRepository = net.ktnx.mobileledger.domain.repository.AccountRepository
typealias CurrencyRepository = net.ktnx.mobileledger.domain.repository.CurrencyRepository
typealias OptionRepository = net.ktnx.mobileledger.domain.repository.OptionRepository
typealias PreferencesRepository = net.ktnx.mobileledger.domain.repository.PreferencesRepository
typealias ProfileRepository = net.ktnx.mobileledger.domain.repository.ProfileRepository
typealias TemplateRepository = net.ktnx.mobileledger.domain.repository.TemplateRepository
typealias TransactionRepository = net.ktnx.mobileledger.domain.repository.TransactionRepository

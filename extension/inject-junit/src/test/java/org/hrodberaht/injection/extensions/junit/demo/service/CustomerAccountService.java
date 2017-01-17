package org.hrodberaht.injection.extensions.junit.demo.service;

import org.hrodberaht.injection.extensions.junit.demo.model.CustomerAccount;

import javax.ejb.Local;

/**
 * Inject extension TDD
 *
 * @author Robert Alexandersson
 *         2011-05-03 21:43
 * @created 1.0
 * @since 1.0
 */
@Local
public interface CustomerAccountService {

    CustomerAccount create(CustomerAccount customer);

    CustomerAccount find(Long id);

    CustomerAccount update(CustomerAccount customerAccount);
}
package com.rt.ehcache.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

/**
 * The persistent class for the address database table.
 * 
 * @author Harry Potter
 *
 */

@Entity
@Table(name = "ADDRESS")
@Cache(usage = CacheConcurrencyStrategy.READ_ONLY, region = "employee")
public class Address implements Serializable {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/** The emp id. */
	@Id
	@Column(name = "empId", unique = true, nullable = false)
	@GeneratedValue(generator = "gen")
	@GenericGenerator(name = "gen", strategy = "foreign", parameters = {
			@Parameter(name = "property", value = "employee") })
	private int empId;

	/** The address line. */
	@Column(name = "addressLine")
	private String addressLine;

	/** The city. */
	@Column(name = "city")
	private String city;

	/** The zipcode. */
	@Column(name = "zipcode")
	private String zipcode;

	/** The employee. */
	@OneToOne
	@PrimaryKeyJoinColumn
	private Employee employee;

	/**
	 * Instantiates a new address.
	 */
	public Address() {
	}

	/**
	 * Gets the emp id.
	 *
	 * @return the emp id
	 */
	public int getEmpId() {
		return empId;
	}

	/**
	 * Sets the emp id.
	 *
	 * @param empId
	 *            the new emp id
	 */
	public void setEmpId(int empId) {
		this.empId = empId;
	}

	/**
	 * Gets the address line.
	 *
	 * @return the address line
	 */
	public String getAddressLine() {
		return this.addressLine;
	}

	/**
	 * Sets the address line.
	 *
	 * @param addressLine
	 *            the new address line
	 */
	public void setAddressLine(String addressLine) {
		this.addressLine = addressLine;
	}

	/**
	 * Gets the city.
	 *
	 * @return the city
	 */
	public String getCity() {
		return this.city;
	}

	/**
	 * Sets the city.
	 *
	 * @param city
	 *            the new city
	 */
	public void setCity(String city) {
		this.city = city;
	}

	/**
	 * Gets the zipcode.
	 *
	 * @return the zipcode
	 */
	public String getZipcode() {
		return this.zipcode;
	}

	/**
	 * Sets the zipcode.
	 *
	 * @param zipcode
	 *            the new zipcode
	 */
	public void setZipcode(String zipcode) {
		this.zipcode = zipcode;
	}

	/**
	 * Gets the employee.
	 *
	 * @return the employee
	 */
	public Employee getEmployee() {
		return this.employee;
	}

	/**
	 * Sets the employee.
	 *
	 * @param employee
	 *            the new employee
	 */
	public void setEmployee(Employee employee) {
		this.employee = employee;
	}

}
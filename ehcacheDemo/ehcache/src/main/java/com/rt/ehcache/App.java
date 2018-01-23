package com.rt.ehcache;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.stat.Statistics;

import com.rt.ehcache.entity.Employee;
import com.rt.ehcache.utils.HibernateUtil;

/**
 * The Class App.
 *
 * @author Harry Potter
 */
public class App {

	/**
	 * The main method.
	 *
	 * @param args
	 *            the arguments
	 */
	public static void main(String[] args) {
		System.out.println("Temp Dir:" + System.getProperty("java.io.tmpdir"));

		// Initialize Sessions
		SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
		Statistics stats = sessionFactory.getStatistics();

		stats.setStatisticsEnabled(true);
		System.out.println("Stats enabled=" + stats.isStatisticsEnabled());

		Session session = sessionFactory.openSession();
		Session otherSession = sessionFactory.openSession();

		Transaction transaction = session.beginTransaction();
		Transaction otherTransaction = otherSession.beginTransaction();

		printStats(stats, 0);

		Employee emp = (Employee) session.load(Employee.class, new Integer(1));
		printData(emp, stats, 1);

		emp = (Employee) session.load(Employee.class, new Integer(1));
		printData(emp, stats, 2);

		// clear first level cache, so that second level cache is used
		session.evict(emp);
		emp = (Employee) session.load(Employee.class, new Integer(1));
		printData(emp, stats, 3);

		emp = (Employee) session.load(Employee.class, new Integer(2));
		printData(emp, stats, 4);

		emp = (Employee) otherSession.load(Employee.class, new Integer(1));
		printData(emp, stats, 5);

		// Release resources
		transaction.commit();
		otherTransaction.commit();
		sessionFactory.close();
	}

	/**
	 * Prints the stats.
	 *
	 * @param stats
	 *            the stats
	 * @param i
	 *            the i
	 */
	private static void printStats(Statistics stats, int i) {
		System.out.println("***** " + i + " *****");
		System.out.println("Fetch Count=" + stats.getEntityFetchCount());
		System.out.println("Second Level Hit Count=" + stats.getSecondLevelCacheHitCount());
		System.out.println("Second Level Miss Count=" + stats.getSecondLevelCacheMissCount());
		System.out.println("Second Level Put Count=" + stats.getSecondLevelCachePutCount());
	}

	/**
	 * Prints the data.
	 *
	 * @param emp
	 *            the emp
	 * @param stats
	 *            the stats
	 * @param count
	 *            the count
	 */
	private static void printData(Employee emp, Statistics stats, int count) {
		System.out.println(count + ":: Name=" + emp.getName() + ", Zipcode=" + emp.getAddress().getZipcode());
		printStats(stats, count);
	}
}

package com.eulerity.hackathon.imagefinder;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.*;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.concurrent.*;

@WebServlet(name = "ImageFinder", urlPatterns = { "/main" })
public class ImageFinder extends HttpServlet {
	// for multithreading
	private String domainName;

	public ImageFinder() {
	}

	public ImageFinder(String urls, String domain) {
		this.domainName = domain;
		crawl(urls);

	}

	private static final long serialVersionUID = 1L;

	protected static final Gson GSON = new GsonBuilder().create();

	private static ArrayList<String> imageUrls = new ArrayList<String>();
	// need to make this shared between threads and if it uses most up to date
	// arraylist then no synchronization needed. Maybe use volatile?
	private static ArrayList<String> vist = new ArrayList<String>();

	// crawl is the caller function of the recusive function crawler
	public void crawl(String url) {
		// parse the initial url and only go to links for that domain,
		// so only go to links with for example (https://www.youtube.com) only go to
		// links with 'youtube' after www.
		// Added declaration of domain and passed into crawler as argument
		// Can make a more sophisticated parse or accomplish the same thing via parsing
		// url from www. to .
		crawler(1, url, vist, this.domainName);

	}

	// This will crawl subpages and keep track of already visited pages, printing
	// out the urls as it visits
	public static void crawler(int level, String url, ArrayList<String> visited, String domain) {
		// public void crawler(int level, String url, String domain) {
		if (url.length() < 12) {
			return;
		}

		// added above if and testDomain declaration, and the &&, and the argument in
		// crawler

		String testDomain = Character.toString(url.charAt(12)) + Character.toString(url.charAt(13));
		if (level <= 5 && testDomain.equals(domain)) {
			Document doc = request(url, visited);

			if (doc != null) {
				for (Element link : doc.select("a[href]")) {
					String next_link = link.absUrl("href");

					if (visited.contains(next_link) == false) {

						crawler(level++, next_link, visited, domain);

					}
				}
			}
		}

	}

	// Method to find images attached to a given url and add them to ArrayList
	// imageUrl
	public static void getImagesAndPrint(String url) {
		Document document;
		try {
			document = Jsoup.connect(url).get();
			// String [] urls={};
			Elements images = document.select("img[src~=(?i)\\.(png|jpe?g|gif)]");
			for (Element image : images) {

				System.out.println("PRINTING... ");
				imageUrls.add(image.attr("src"));

				System.out.println("Image Source: " + image.attr("src"));
				System.out.println("Image Height: " + image.attr("height"));
				System.out.println("Image Width" + image.attr("width"));
			}
			// resp.getWriter().print(GSON.toJson(urlInList));

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static Document request(String url, ArrayList<String> v) {
		// public Document request(String url) {
		try {
			System.out.println("HI IM HERE   " + url);
			// found the error, sometimes its getting empty url here
			if (!url.equals("")) {
				Connection con = Jsoup.connect(url);

				System.out.println("HI NOW IM HERE");
				Document doc = con.get();

				if (con.response().statusCode() == 200) {
					// getImagesAndPrint(String url), will take url as input and add to ArrayList
					// imageUrls the images on that page

					// call function here!
					// takes in a list of strings as url
					System.out.println("Hello me again");
					getImagesAndPrint(url);

					System.out.println("Link: " + url);

					System.out.println(doc.title());
					v.add(url);
					return doc;
				}
			}
			return null;
		} catch (IOException e) {
			return null;
		}

	}

	// method to get list of subpages of the first url to start threads on remove if
	// not working

	public static ArrayList<String> getConnections(String url, ArrayList<String> connections) {
		if (url != null) {
			// get multiple subpages on the first url
			// create multithread objects
			// call thread start on them
			Document doc;
			try {
				Connection con = Jsoup.connect(url);
				doc = con.get();
				if (con.response().statusCode() == 200) {
					Elements links = doc.select("a[href]");

					for (Element link : links) {
						connections.add(link.attr("href"));
					}
				}

			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		return connections;
	}

	@Override
	protected final void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		resp.setContentType("text/json");
		String path = req.getServletPath();
		String url = req.getParameter("url");
		System.out.println("Got request of:" + path + " with query param:" + url);

		imageUrls.clear();
		// Gets url to scrape
		// call function with this url
		// print out with function below

		// okay so make a function to scrape images which takes in url as String and
		// returns a list of strings(images), then pass into this function to print
		// Make it crawl to subpages and find more within the url's domain
		// avoid recrawling to pages you've already visited
		// then make that function multithreaded

		// So crawl works in finding subpages and keeping track of visited.
		// Now we need to make it get the images from those pages and put into a list of
		// strings (String [])
		// Can save the image urls in a class variable that resets in this function
		// because it gets called again every post
		System.out.println("\n\n\n\n\nURL IS: " + url);

		ArrayList<String> connections = new ArrayList<String>();
		// get multiple subpages on the first url
		connections = getConnections(url, connections);
		// create multithread objects
		// and call thread start
		// add thread amount limit
		if (url == null) {
			return;
		}
		// get domain name
		String domains = Character.toString(url.charAt(12)) + Character.toString(url.charAt(13));
		ExecutorService es = Executors.newCachedThreadPool();
		int count = 0;
		for (int i = 0; i < connections.size(); i++) {
			es.execute(new Thread(new multithreading(connections.get(i), domains)));
			count++;
		}

		es.shutdown();
		try {
			boolean finished = es.awaitTermination(1, TimeUnit.MINUTES);

		} catch (InterruptedException e) {

		}

		System.out.println(imageUrls.size());
		System.out.println(imageUrls.toString());

		// add logo detection here
		// get a count of how many of each image url there are
		HashMap<String, Integer> hash = new HashMap<String, Integer>();
		for (String str : imageUrls) {
			if (hash.containsKey(str)) {
				hash.put(str, hash.get(str) + 1);
			} else {
				hash.put(str, 1);
			}
		}
		// if a image url appears very often then it has a high chance of being a logo
		ArrayList<String> logos = new ArrayList<String>();
		for (String logo : hash.keySet()) {
			// ideally set a number, a hard limit. But for now since threads are visiting
			// already visited by other threads by not by the thread itself.
			// increasing the number would make it less likely to get non-logos but may miss
			// some logos
			if (hash.get(logo) > count / 2) {
				logos.add(logo);
			}

		}
		System.out.println("\n\n\n\nLikely logos: " + logos.toString());
		System.out.println(logos.size());

		resp.getWriter().print(GSON.toJson(imageUrls));

	}

}

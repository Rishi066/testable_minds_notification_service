package com.notifier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Cookie;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

public class LoginSetup
{
    public static final String COOKIE_FILE = "cookies.json";

    public static void main(String[] args) throws IOException
    {
        Scanner sc = new Scanner(System.in);
        System.out.print("[!] METHOD OF LOGIN [!]\n");
        System.out.print("[1]: Sign in via Credentials\n[2]: Sign in via Google/Facebook\n");
        System.out.print("[!] Enter your choice number: ");
        int choice = -1;

        try
        {
             choice = sc.nextInt();
        }
        catch(InputMismatchException e)
        {
            System.out.print("[!] INVALID CHOICE. PLEASE RUN THE PROGRAM AGAIN [!]");
            System.exit(0);
        }
        sc.nextLine();

        switch(choice)
        {
            case 1 :
                System.out.print("\nPLEASE WAIT BROWSER WILL OPEN\n");
                try(Playwright playwright = Playwright.create())
                {
                    Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setChannel("chrome").setHeadless(false));
                    BrowserContext context = browser.newContext();
                    Page page = context.newPage();

                    page.navigate("https://minds.testable.org/login");

                    System.out.println("\n\nA browser window has been opened");

                    System.out.println("\n\nLogin to your TM account in the opened window");
                    System.out.println("\n\nOnce you have logged in, press ENTER");

                    sc.nextLine();

                    List<Cookie> cookies = context.cookies();
                    List<Cookie> filteredCookies = new ArrayList<>();
                    for(Cookie cookie : cookies)
                    {
                        if(cookie.name.equals("XSRF-TOKEN") || cookie.name.equals("testable_minds_session"))
                        {
                            filteredCookies.add(cookie);
                        }
                    }

                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                    try(FileWriter writer = new FileWriter(COOKIE_FILE))
                    {
                        gson.toJson(filteredCookies,writer);
                    }

                    System.out.println("Saved " + filteredCookies.size() + " cookies to " + COOKIE_FILE);
                    browser.close();
                }
                break;

            case 2:
                sc = new Scanner(System.in);
                System.out.println("Enter the session cookie extracted: ");
                List<Cookie> cookies = new ArrayList<>();
                cookies.add(new Cookie("testable_minds_session",sc.nextLine()));

                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                try(FileWriter writer = new FileWriter(COOKIE_FILE))
                {
                    gson.toJson(cookies,writer);
                }

                System.out.println("Saved " + cookies.size() + " cookies to " + COOKIE_FILE);
                break;

            default: System.out.println("[!] INVALID CHOICE. PLEASE RUN THE PROGRAM AGAIN [!]");
            break;
        }
    }
}

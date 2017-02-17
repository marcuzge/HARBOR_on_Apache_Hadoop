# HARBOR_on_Apache_Hadoop

As part of the larger CHAOS project, the mysterious No Such Agency has effectively wiretapped a huge fraction of the internet, recording every page request and, for web traffic that identifies the user, also recording the username in the reply. Each record includes both a timestamp and the network 4-tuple (source IP, source port, destination IP, destination port). For requests, it includes any tracking cookie seen in the HTTP request, while web replies include any extracted username. Requests also include the full HTTP headers (which include other features), but for this purpose they are simply treated as an opaque blob of data and excluded from the analysis.

Of course, simply recording this data is of little use; it needs to be both analyzed and searched. Your job is to prototype the HARBOR internal database structure using Hadoop. HARBOR performs three primary tasks:

1: It matches requests and replies: When the data includes both a request and reply with the same network 4-tuple with a timestamp that is within 10 seconds, it creates a stream of matched records which include both the request and reply.

2: It writes the output stream of matched request/reply pairs into a set of independent "Query Focused Datasets" (QFDs). In a QFD, a particular key (such as the source IP, destination IP, cookie, or username) is hashed using a cryptographic hash function. Then the lowest 2 bytes are used to select which file to write into, with the resulting file containing both the hash and the data.

This enables efficient searching for
All records associated with a username
All records associated with a tracking cookie
All records associated with a source IP or a destination IP
The particular QFD keys we will be using are:
cookie: Cookies included in user requests
srcIP: Source IP addresses
destIP: Destination IP addresses
3: It provides a second tool to create the TOTALFAIL database of Tor users (the torusers QFD). This tool first accesses the QFD which indexes matched users by source IP to get the users who are seen using a set of Tor exit nodes. Using any tracking cookies present in the resulting query, it accesses the tracking cookie QFD to get a list of all users, and then does a query for the usernames associated with those tracking cookies to create the TOTALFAIL QFD.

This is a structure optimized for the particular problem faced by the No Such Agency. They are obtaining a massive amount of data, recording the activity of a huge fraction of people on the planet. Most of this data is completely irrelevant, but it is not known in advance which is relevant or not.

Instead, the No Such Agency desires the capability to easily query "At this IP, who was connected to the network" and "For this person, which IPs and when did he connect to the network."

Query Focused Datasets solve this problem. In a parallel structure like Hadoop, access time is dominated by disk latency and network latency. By splitting the data into buckets, this can be efficiently parallelized (with each bucket potentially written by a separate process) and easily communicated in parallel. Then when any particular entry is needed, only that particular bucket needs to be searched for the resulting value.

Within a bucket we don't bother sorting: since most buckets will never be read, and since accessing a bucket is dominated by the time it takes to load a bucket from disk (rather than to search for an entry within a bucket) it is better from a total cost perspective to not bother sorting the data.

Then the TOTALFAIL analysis is simply taking advantage of this in a parallel structure. With the initial QFDs in place, it's easy to do a query for "all Tor users" based on the IPs used by Tor, and then do a query for "each of these users, what is their activity." Now when a No Such Agency analyst wants to find out more about a particular Tor user, they can easily discover all their activity.

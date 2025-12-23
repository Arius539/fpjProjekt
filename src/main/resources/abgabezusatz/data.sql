--
-- PostgreSQL database dump
--

-- Dumped from database version 14.1
-- Dumped by pg_dump version 17.3

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.users (id, username, password_hash, created_at) FROM stdin;
28	max.mustermann@gmail.com	$2a$10$XofMf5MnGNnt5gdv6IfyWuTa9Uh45LhiG.SE.q/30a/kYNeAyGY7C	2025-12-23 17:05:47.018827+00
29	kevin.schulz@gmail.de	$2a$10$SqafqWHyZVpmQt4I.J.5bunTi1B20C65hkdEP7jcVU96DAZsSpjAa	2025-12-23 17:11:28.020513+00
30	timojahn@web.de	$2a$10$E6Xx/w73G20KrVLUtf/WWu2p/A4Pi9Sv/6JgWnsCWX6iZjVA0T2W2	2025-12-23 17:12:55.166368+00
\.


--
-- Data for Name: direct_messages; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.direct_messages (id, sender, recipient, content, created_at) FROM stdin;
1140082	30	28	Hallo. Wie geht's?	2025-12-23 17:14:44.027515+00
1140083	29	30	Danke für das Weihnachtsgeschenk	2025-12-23 17:16:57.715776+00
1140084	30	29	Gerne. Schön dass es dich freut 😊	2025-12-23 17:17:41.678321+00
\.


--
-- Data for Name: pinboard_comments; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.pinboard_comments (id, wall_owner_id, author_id, content, created_at) FROM stdin;
112	30	29	Hallo, ich bin neu hier	2025-12-23 17:14:05.450576+00
113	28	29	:)	2025-12-23 17:14:36.795136+00
114	29	30	Gutes Produkt	2025-12-23 17:15:47.650661+00
115	28	30	Nett hier	2025-12-23 17:16:33.724683+00
\.


--
-- Data for Name: transactions; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.transactions (id, transaction_type, amount, sender, recipient, description, created_at) FROM stdin;
1001257	EINZAHLUNG	1000.00	\N	28	Startguthaben	2025-12-23 17:07:05.618896+00
1001258	EINZAHLUNG	1000.00	\N	29	Weihnachtsgeld	2025-12-23 17:12:18.216388+00
1001261	UEBERWEISUNG	100.00	29	28	Geschenk	2025-12-23 17:15:16.024+00
1001259	EINZAHLUNG	2000.00	\N	30	Startguthaben	2025-12-23 17:13:45.195914+00
1001260	UEBERWEISUNG	100.00	30	29	Weihnachtsgeschenk	2025-12-23 17:14:07.260408+00
\.


--
-- Name: direct_messages_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.direct_messages_id_seq', 1140084, true);


--
-- Name: pinboard_comments_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.pinboard_comments_id_seq', 115, true);


--
-- Name: transactions_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.transactions_id_seq', 1001261, true);


--
-- Name: users_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.users_id_seq', 30, true);


--
-- PostgreSQL database dump complete
--


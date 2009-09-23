insert into USERS(USERID,VERSION,name,realName,password) values(1,1,'vralev','Vladimir Ralev','vralev');
insert into USERS(USERID,VERSION,name,realName,password) values(2,1,'contact','Contact','contact');
insert into USERS(USERID,VERSION,name,realName,password) values(3,1,'admin','Admin','admin');
insert into USERS(USERID,VERSION,name,realName,password) values(4,1,'vnaudion','Vincent Naudion','vnaudion');
insert into USERS(USERID,VERSION,name,realName,password) values(5,1,'smichea','S�bastien Mich�a','smichea');

insert into CONTACTS(CONTACTID,VERSION,USERID,uri,name) values(1,1,1,'sip:contact@localhost:5060','Local on port 5060');
insert into CONTACTS(CONTACTID,VERSION,USERID,uri,name) values(2,1,1,'sip:contact@192.168.1.187:5060', 'Wifi on port 5060');
insert into CONTACTS(CONTACTID,VERSION,USERID,uri) values(3,1,1,'sip:contact@localhost:5064');
insert into CONTACTS(CONTACTID,VERSION,USERID,uri) values(4,1,2,'sip:contact@192.168.1.187:5064');
insert into CONTACTS(CONTACTID,VERSION,USERID,uri,name) values(5,1,1,'18004664411','Google Phone Search');
insert into CONTACTS(CONTACTID,VERSION,USERID,uri) values(6,1,1,'sip:twinkle@192.168.1.187:5066');

insert into CONTACTS(CONTACTID,VERSION,USERID,uri,name) values(7,1,4,'sip:smichea@88.191.37.155:5060','Home smichea');
insert into CONTACTS(CONTACTID,VERSION,USERID,uri,name) values(8,1,4,'sip:vralev@88.191.37.155:5060','Home vralev');
insert into CONTACTS(CONTACTID,VERSION,USERID,uri,name) values(9,1,4,'0033685048335','Mobile vnaudion');
insert into CONTACTS(CONTACTID,VERSION,USERID,uri,name) values(10,1,4,'18004664411','Google Phone Search');
insert into CONTACTS(CONTACTID,VERSION,USERID,uri,name) values(11,1,5,'18004664411','Google Phone Search');

-- Here is the deal - you must keep the id sequience correct, otherwise the id-generator
-- in hibernate gets confused and generates duplicate ids and you skip say 3 entries you will get
-- a tx failure 3 times. It will never happen again because then it will scan though all entries.
-- This can be fixed by using another id generator, but this is usually db-specific like the hsql generator.
insert into REGISTRATIONS(REGISTRATIONID,VERSION,USERID,selected,uri,name) values(1,1,1,false,'sip:vralev@localhost','Home Phone');
insert into REGISTRATIONS(REGISTRATIONID,VERSION,USERID,selected,uri,name) values(2,1,1,false,'sip:vralev@192.168.1.187','Office Phone');
insert into REGISTRATIONS(REGISTRATIONID,VERSION,USERID,selected,uri) values(3,1,1,false,'sip:vralev@localhost:5064');
insert into REGISTRATIONS(REGISTRATIONID,VERSION,USERID,selected,uri) values(4,1,1,true,'359888579097');
insert into REGISTRATIONS(REGISTRATIONID,VERSION,USERID,selected,uri) values(5,1,1,false,'sip:vralev-ekiga@192.168.1.187:5061');
insert into REGISTRATIONS(REGISTRATIONID,VERSION,USERID,selected,uri) values(6,1,1,false,'sip:receiver@sip-servlets.com');

insert into REGISTRATIONS(REGISTRATIONID,VERSION,USERID,selected,uri,name) values(7,1,4,false,'sip:vnaudion@localhost','Home Phone');
insert into REGISTRATIONS(REGISTRATIONID,VERSION,USERID,selected,uri,name) values(8,1,4,false,'0033685048335','Mobile Phone');

insert into REGISTRATIONS(REGISTRATIONID,VERSION,USERID,selected,uri,name) values(9,1,5,false,'sip:smichea@localhost','Home Phone');

--insert into BINDINGS(BINDINGID,VERSION,CONTACTADDRESS,CALLID,CSEQ,EXPIRES,REGISTRATIONID) values(1,1,'sip:vralev@localhost:5060','cid1',1,5000,1);
--insert into BINDINGS(BINDINGID,VERSION,CONTACTADDRESS,CALLID,CSEQ,EXPIRES,REGISTRATIONID) values(2,1,'sip:vralev@192.168.1.187:5060','sdfd2s',2,5200,1);
--insert into BINDINGS(BINDINGID,VERSION,CONTACTADDRESS,CALLID,CSEQ,EXPIRES,REGISTRATIONID) values(3,1,'sip:vralev@192.168.1.187:5064','sdfd2s',2,5200,2);
--insert into BINDINGS(BINDINGID,VERSION,CONTACTADDRESS,CALLID,CSEQ,EXPIRES,REGISTRATIONID) values(4,1,'sip:vralev@localhost:5064','sdfd2s',2,5200,2);

insert into HISTORYLOG(HISTORYID,VERSION,USERID,MESSAGE, TIMESTAMP) values(1,1,1,'Missed call from sip:c1@swdsd.com','2009-01-14 11:39:00');
insert into HISTORYLOG(HISTORYID,VERSION,USERID,MESSAGE, TIMESTAMP) values(2,1,1,'Answered call from sip:c1@swdsd.com','2009-01-14 16:39:00');

insert into ROLES(ROLEID,VERSION,USERID,role) values(1,1,1,'caller');
insert into ROLES(ROLEID,VERSION,USERID,role) values(2,1,1,'admin');
insert into ROLES(ROLEID,VERSION,USERID,role) values(3,1,2,'caller');
insert into ROLES(ROLEID,VERSION,USERID,role) values(4,1,2,'admin');
insert into ROLES(ROLEID,VERSION,USERID,role) values(5,1,3,'caller');
insert into ROLES(ROLEID,VERSION,USERID,role) values(6,1,3,'admin');
insert into ROLES(ROLEID,VERSION,USERID,role) values(7,1,4,'caller');
insert into ROLES(ROLEID,VERSION,USERID,role) values(8,1,4,'admin');
insert into ROLES(ROLEID,VERSION,USERID,role) values(9,1,5,'caller');
insert into ROLES(ROLEID,VERSION,USERID,role) values(10,1,5,'admin');

--insert into GLOBALPROPERTIES(GLOBALPROPERTYID, VERSION, NAME, VALUE) values (1,1,'pbx.hostname','localhost')
--insert into GLOBALPROPERTIES(GLOBALPROPERTYID, VERSION, NAME, VALUE) values (2,1,'pbx.call.timeout','80000')
--insert into GLOBALPROPERTIES(GLOBALPROPERTYID, VERSION, NAME, VALUE) values (3,1,'pbx.default.ringback.tone','http://mobicents.googlecode.com/svn/trunk/applications/ipbx/audio/ringback.wav')
--insert into GLOBALPROPERTIES(GLOBALPROPERTYID, VERSION, NAME, VALUE) values (4,1,'pbx.default.rejected.announcement','rejected.wav')
--insert into GLOBALPROPERTIES(GLOBALPROPERTYID, VERSION, NAME, VALUE) values (5,1,'pbx.default.muted.announcement','http://mobicents.googlecode.com/svn/trunk/applications/ipbx/audio/muted.wav')
--insert into GLOBALPROPERTIES(GLOBALPROPERTYID, VERSION, NAME, VALUE) values (6,1,'pbx.default.unmuted.announcement','http://mobicents.googlecode.com/svn/trunk/applications/ipbx/audio/unmuted.wav')
--insert into GLOBALPROPERTIES(GLOBALPROPERTYID, VERSION, NAME, VALUE) values (7,1,'pbx.default.switched.announcement','http://mobicents.googlecode.com/svn/trunk/applications/ipbx/audio/switched.wav')
--insert into GLOBALPROPERTIES(GLOBALPROPERTYID, VERSION, NAME, VALUE) values (8,1,'pbx.default.everyone.announcement','http://mobicents.googlecode.com/svn/trunk/applications/ipbx/audio/everyone.wav')
--insert into GLOBALPROPERTIES(GLOBALPROPERTYID, VERSION, NAME, VALUE) values (9,1,'pbx.registration.strict','false') -- If we should parse registrations by uri or just by name (the first part of the URI
--insert into GLOBALPROPERTIES(GLOBALPROPERTYID, VERSION, NAME, VALUE) values (10,1,'pbx.default.onhold.announcement','http://mobicents.googlecode.com/svn/trunk/applications/ipbx/audio/muted.wav')

insert into GLOBALPROPERTIES(GLOBALPROPERTYID, VERSION, NAME, VALUE) values (1,1,'pbx.hostname','localhost')
insert into GLOBALPROPERTIES(GLOBALPROPERTYID, VERSION, NAME, VALUE) values (2,1,'pbx.call.timeout','80000')
insert into GLOBALPROPERTIES(GLOBALPROPERTYID, VERSION, NAME, VALUE) values (3,1,'pbx.default.ringback.tone','http://mobicents.googlecode.com/svn/trunk/applications/ipbx/audio/ringback.wav')
insert into GLOBALPROPERTIES(GLOBALPROPERTYID, VERSION, NAME, VALUE) values (4,1,'pbx.default.rejected.announcement','rejected.wav')
insert into GLOBALPROPERTIES(GLOBALPROPERTYID, VERSION, NAME, VALUE) values (5,1,'pbx.default.muted.announcement','http://mobicents.googlecode.com/svn/trunk/applications/ipbx/audio/muted.wav')
insert into GLOBALPROPERTIES(GLOBALPROPERTYID, VERSION, NAME, VALUE) values (6,1,'pbx.default.unmuted.announcement','http://mobicents.googlecode.com/svn/trunk/applications/ipbx/audio/unmuted.wav')
insert into GLOBALPROPERTIES(GLOBALPROPERTYID, VERSION, NAME, VALUE) values (7,1,'pbx.default.switched.announcement','http://mobicents.googlecode.com/svn/trunk/applications/ipbx/audio/switched.wav')
insert into GLOBALPROPERTIES(GLOBALPROPERTYID, VERSION, NAME, VALUE) values (8,1,'pbx.default.everyone.announcement','http://mobicents.googlecode.com/svn/trunk/applications/ipbx/audio/everyone.wav')
insert into GLOBALPROPERTIES(GLOBALPROPERTYID, VERSION, NAME, VALUE) values (9,1,'pbx.registration.strict','false') -- If we should parse registrations by uri or just by name (the first part of the URI
insert into GLOBALPROPERTIES(GLOBALPROPERTYID, VERSION, NAME, VALUE) values (10,1,'pbx.default.onhold.announcement','http://mobicents.googlecode.com/svn/trunk/applications/ipbx/audio/muted.wav')
insert into GLOBALPROPERTIES(GLOBALPROPERTYID, VERSION, NAME, VALUE) values (11,1,'pbx.headless.phone.prefix','sip:phone')

insert into PSTNGATEWAYACCOUNTS(PSTNACCOUNTID, VERSION, NAME, USERNAME, HOSTNAME, PASSWORD) values(1,1,'Callwithus account','189964505','sip.callwithus.com','412944')
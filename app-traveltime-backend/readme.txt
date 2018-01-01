This is the backend for the traveltime android app.

Its role is to receive the push messages from google calendar
then dispatch a message to firebase, firebase will then notify
the client it needs to resync the calendar

Endpoints

/register - POST
	Create a user and password

/channel/new - POST and Auth
	Return info needed by client to create new push channel

/channel/unsub/{channelId} - POST and Auth
	Return info needed by client to unsubcribe from push channel

/channel/notify/{channelId} - POST
	Google calendar push notification target



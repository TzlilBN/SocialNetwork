
#include <../include/StompConnectionHandler.h>
#include <StompProtocol.h>

using boost::asio::ip::tcp;

using std::cin;
using std::cout;
using std::cerr;
using std::endl;
using std::string;

StompConnectionHandler::StompConnectionHandler(string host, short port) : host_(host), port_(port), io_service_(),
                                                                socket_(io_service_), pro(new StompProtocol()){}

StompConnectionHandler::~StompConnectionHandler() {
	pro->~StompProtocol();
	close();
}

bool StompConnectionHandler::connect() {
	std::cout << "Starting connect to "
	          << host_ << ":" << port_ << std::endl;
	try {
		tcp::endpoint endpoint(boost::asio::ip::address::from_string(host_), port_); // the server endpoint
		boost::system::error_code error;
		socket_.connect(endpoint, error);
		if (error)
			throw boost::system::system_error(error);
	}
	catch (std::exception &e) {
		std::cerr << "Connection failed (Error: " << e.what() << ')' << std::endl;
		return false;
	}
	return true;
}

bool StompConnectionHandler::getBytes(char bytes[], unsigned int bytesToRead) {
	size_t tmp = 0;
	boost::system::error_code error;
	try {
		while (!error && bytesToRead > tmp) {
			tmp += socket_.read_some(boost::asio::buffer(bytes + tmp, bytesToRead - tmp), error);
		}
		if (error)
			throw boost::system::system_error(error);
	} catch (std::exception &e) {
		std::cerr << "recv failed (Error: " << e.what() << ')' << std::endl;
		return false;
	}
	return true;
}

bool StompConnectionHandler::sendBytes(const char bytes[], int bytesToWrite) {
	int tmp = 0;
	boost::system::error_code error;
	try {
		while (!error && bytesToWrite > tmp) {
			tmp += socket_.write_some(boost::asio::buffer(bytes + tmp, bytesToWrite - tmp), error);
		}
		if (error)
			throw boost::system::system_error(error);
	} catch (std::exception &e) {
		std::cerr << "recv failed (Error: " << e.what() << ')' << std::endl;
		return false;
	}
	return true;
}

bool StompConnectionHandler::getFrame(std::string &line) {
	bool ret = getFrameAscii(line, '\0');
	if (!ret)
		return ret;
	pro->recieveAndAct(line);
	return ret;
}

bool StompConnectionHandler::sendFrame(std::string &line) {
	string output = pro->prepareSendToServer(line);
	if (output.compare("already logged") == 0)
		return true;
	if (!output.compare("report") == 0 && !output.compare("summary") == 0)
		return sendFrameAscii(output, '\0');
	if (output.compare("summary") == 0)
	{
		pro->summary(line);
		return true;
	}
	vector<string>* events = pro->report(line);
	for (string event: *events)
		sendFrameAscii(event, '\0');
	return true;

}


bool StompConnectionHandler::getFrameAscii(std::string &frame, char delimiter) {
	char ch;
	// Stop when we encounter the null character.
	// Notice that the null character is not appended to the frame string.
	try {
		do {
			if (!getBytes(&ch, 1)) {
				return false;
			}
			if (ch != '\0')
				frame.append(1, ch);
		} while (delimiter != ch);
	} catch (std::exception &e) {
		std::cerr << "recv failed2 (Error: " << e.what() << ')' << std::endl;
		return false;
	}
	return true;
}

bool StompConnectionHandler::sendFrameAscii(const std::string &frame, char delimiter) {
	bool result = sendBytes(frame.c_str(), frame.length());
	if (!result) return false;
	return sendBytes(&delimiter, 1);
}

// Close down the connection properly.
void StompConnectionHandler::close() {
	try {
		socket_.close();
	} catch (...) {
		std::cout << "closing failed: connection already closed" << std::endl;
	}
}

void StompConnectionHandler::run()
{
	while (1)
	{
		if (!pro->getTerminate()){
			string answer = "";
			if (!getFrame(answer)) {
				std::cout << "Disconnected. Exiting...\n" << std::endl;
				return;
			}
		}
		else
		{
			close();
			break;
		}
	}

}
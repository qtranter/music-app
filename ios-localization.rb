require 'fileutils'

# Defines the list of supported languages by specifying the iOS language code and the Android folder name where strings are located
languages = {
	"en" => "values",
	"es" => "values-es-rES",
	"fr" => "values-fr",
	"zh-Hans" => "values-zh-rCN",
	"th" => "values-th",
	"ru" => "values-ru",
	"pt-PT" => "values-pt-rPT",
	"pt-BR" => "values-pt-rBR",
	"nl" => "values-nl",
	"ms" => "values-ms",
	"ko" => "values-ko",
	"ja" => "values-ja",
	"it" => "values-it",
	"id" => "values-in",
	"hi" => "values-hi",
	"es-419" => "values-b+es+419",
	"de" => "values-de",
	"da" => "values-da",
	"ar" => "values-ar"
}

# Generates a date signature to be added to the exported files
dateSignature = "// Generated on #{Time.now}\n"	

# Prepares directories to store the exported files
iOSDir = "ios-strings"
FileUtils.rm_rf(iOSDir)
Dir.mkdir iOSDir

englishData = {}

languages.map do |iosLanguageKey, androidFolderName|

    reference = iosLanguageKey == "en"

	iOSLanguageDir = "#{iOSDir}/#{iosLanguageKey}.lproj"
	Dir.mkdir iOSLanguageDir

	# Reads and transforms Android strings to iOS-friendly key/value pairs
	strings = englishData
	File.foreach("AM/src/main/res/#{androidFolderName}/strings.xml") { |line|
		strippedLine = line.strip
		if strippedLine.start_with? "<string"
			key = "\"#{strippedLine.split('"')[1]}\""
			value = strippedLine[strippedLine.index(">") + 1 .. strippedLine.rindex("<") - 1]
				.gsub("\\'", "'")
				.gsub("&amp;", "&")
				.gsub("%s", "%@")
				.gsub("&#13;", "")
			strings[key] = value
		end
	}

    if reference
        englishData = strings
    end

	# Writes permissions strings to IntoPlist.strings
	File.open("#{iOSLanguageDir}/InfoPlist.strings", "w") { |output|
		output.write dateSignature
		strings.select { |key, value| (key.start_with? "\"NS") || (key.start_with? "\"shortcuts_") }.sort.map do |key,value|
			output.write "#{key}=\"#{value}\";\n"
		end
	}

	# Writes all remaining strings to Localizable.strings
	File.open("#{iOSLanguageDir}/Localizable.strings", "w") { |output|
		output.write dateSignature
		strings.select { |key, value| (!key.start_with? "\"NS") && (!key.start_with? "\"shortcuts_") }.sort.map do |key,value|
			output.write "#{key}=\"#{value}\";\n"
		end
	}

end

# Creates a zip with the iOS data
zipFile = "#{iOSDir}.zip"
result = system("zip -r #{zipFile} #{iOSDir} > /dev/null")

# Deletes the folder that has been compressed
FileUtils.rm_rf(iOSDir)

puts "✅ Conversion completed --> #{zipFile}"
package no.fint.drosjeloyve

import no.fint.drosjeloyve.model.AltinnApplication
import no.fint.drosjeloyve.util.ContentDetector
import spock.lang.Specification

class ContentDetectorSpec extends Specification {

    def "given valid media type and content returns actual media type"() {
        given:
        def bytes = getClass().getClassLoader().getResourceAsStream('skjermbilde.png').bytes
        def attachment = new AltinnApplication.Attachment(attachmentType: 'binary/octet-stream')

        when:
        def mediaType = ContentDetector.getMediaType(bytes, attachment)

        then:
        mediaType == 'image/png'
    }

    def "given valid media type and unrecognized content returns octet stream"() {
        given:
        def bytes = new byte[0]
        def attachment = new AltinnApplication.Attachment(attachmentType: 'binary/octet-stream')

        when:
        def mediaType = ContentDetector.getMediaType(bytes, attachment)

        then:
        mediaType == 'application/octet-stream'
    }

    def "given filename with extension returns filename with actual extension"() {
        given:
        def bytes = getClass().getClassLoader().getResourceAsStream('skjermbilde.png').bytes
        def attachment = new AltinnApplication.Attachment(fileName: 'filename.pxg', attachmentTypeName: 'attachment-type-name')
        def mediaType = ContentDetector.getMediaType(bytes, attachment)

        when:
        def extension = ContentDetector.getFileName(mediaType, attachment)

        then:
        extension == 'attachment-type-name.png'
    }

    def "given filename without extension returns filename with actual extension"() {
        given:
        def bytes = getClass().getClassLoader().getResourceAsStream('skjermbilde.png').bytes
        def attachment = new AltinnApplication.Attachment(fileName: 'filename', attachmentTypeName: 'attachment-type-name')
        def mediaType = ContentDetector.getMediaType(bytes, attachment)

        when:
        def extension = ContentDetector.getFileName(mediaType, attachment)

        then:
        extension == 'attachment-type-name.png'
    }

    def "given filename without extension returns filename with taxi extensionn"() {
        given:
        def attachment = new AltinnApplication.Attachment(fileName: 'filename', attachmentTypeName: 'attachment-type-name')
        def mediaType = 'binary/octet-stream'

        when:
        def extension = ContentDetector.getFileName(mediaType, attachment)

        then:
        extension == 'attachment-type-name.taxi'
    }
}
